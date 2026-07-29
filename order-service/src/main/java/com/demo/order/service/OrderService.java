package com.demo.order.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.OrderResponse;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;
import com.demo.order.event.OrderEvent;
import com.demo.order.exception.InvalidOrderStateException;
import com.demo.order.exception.OrderNotFoundException;
import com.demo.order.outbox.OutboxEvent;
import com.demo.order.outbox.OutboxEventRepository;
import com.demo.order.repository.OrderRepository;
import com.demo.order.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Order management service demonstrating:
 * <ul>
 * <li>{@code CompletableFuture} with Java 21 Virtual Thread executor</li>
 * <li>{@code @Cacheable/@CacheEvict} via Spring Boot auto-configured Redis</li>
 * <li>Kafka event publishing (auto-configured producer)</li>
 * <li>Pattern matching switch for state machine transitions</li>
 * </ul>
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

	private static final String ORDER_CACHE = "orders";

	private final OrderRepository orderRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;
	private final OrderMapper orderMapper;

	/**
	 * Java 21 Virtual Thread executor — lightweight threads for I/O-bound parallel
	 * tasks.
	 */
	private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

	public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository,
			ObjectMapper objectMapper, OrderMapper orderMapper) {
		this.orderRepository = orderRepository;
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
		this.orderMapper = orderMapper;
	}

	// ── Create ──────────────────────────────────────────────

	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		long userOrderCount = orderRepository.countByUserId(request.userId());
		String orderNumber = "ORD-" + request.userId() + "-" + (userOrderCount + 1);

		Order order = Order.builder().orderNumber(orderNumber).userId(request.userId()).productId(request.productId())
				.quantity(request.quantity()).totalPrice(request.totalPrice()).notes(request.notes())
				.status(OrderStatus.PENDING).build();

		order = orderRepository.save(order);
		log.info("Order created: id={}, userId={}", order.getId(), order.getUserId());

		// Outbox Pattern: Save event in the same transaction
		saveOutboxEvent(
				OrderEvent.created(order.getId(), order.getUserId(), order.getProductId(), order.getQuantity()));

		return orderMapper.toResponse(order);
	}

	/**
	 * Async order creation using {@link CompletableFuture} on a Virtual Thread.
	 */
	public CompletableFuture<OrderResponse> createOrderAsync(CreateOrderRequest request) {
		return CompletableFuture.supplyAsync(() -> createOrder(request), virtualExecutor);
	}

	/**
	 * Bulk order creation — each order runs in parallel on its own Virtual Thread.
	 * Demonstrates {@code CompletableFuture.allOf()} for concurrent batch
	 * operations.
	 */
	public List<OrderResponse> createOrdersBulk(List<CreateOrderRequest> requests) {
		List<CompletableFuture<OrderResponse>> futures = requests.stream()
				.map(req -> CompletableFuture.supplyAsync(() -> createOrder(req), virtualExecutor)).toList();

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		return futures.stream().map(CompletableFuture::join).toList();
	}

	// ── Read ────────────────────────────────────────────────

	@Cacheable(value = ORDER_CACHE, key = "#id")
	public OrderResponse getOrderById(Long id) {
		return orderRepository.findById(id).map(orderMapper::toResponse)
				.orElseThrow(() -> new OrderNotFoundException(id.toString()));
	}

	public List<OrderResponse> getOrdersByUser(Long userId) {
		return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(orderMapper::toResponse).toList();
	}

	public org.springframework.data.domain.Page<OrderResponse> getAllOrders(
			org.springframework.data.domain.Pageable pageable) {
		return orderRepository.findAll(pageable).map(orderMapper::toResponse);
	}

	/**
	 * Parallel fetch for multiple users using CompletableFuture on virtual threads.
	 */
	public List<OrderResponse> getOrdersForMultipleUsers(List<Long> userIds) {
		List<CompletableFuture<List<OrderResponse>>> futures = userIds.stream()
				.map(uid -> CompletableFuture.supplyAsync(() -> getOrdersByUser(uid), virtualExecutor)).toList();

		return futures.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
	}

	// ── Update ──────────────────────────────────────────────

	@Transactional
	@CacheEvict(value = ORDER_CACHE, key = "#id")
	public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
		Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id.toString()));
		validateTransition(order.getStatus(), newStatus);
		order.setStatus(newStatus);
		order = orderRepository.save(order);
		log.info("Order {} status updated to {}", id, newStatus);

		// Outbox Pattern
		saveOutboxEvent(OrderEvent.statusChanged(order.getId(), order.getUserId(), newStatus.name()));

		return orderMapper.toResponse(order);
	}

	@Transactional
	@CacheEvict(value = ORDER_CACHE, key = "#id")
	public OrderResponse cancelOrder(Long id) {
		return updateOrderStatus(id, OrderStatus.CANCELLED);
	}
	
	@Transactional
	@CacheEvict(value = ORDER_CACHE, key = "#id")
	public OrderResponse refundOrder(Long id, com.demo.order.dto.RefundRequest request) {
		Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id.toString()));
		
		// Proactive refund validation: usually allowed only when DELIVERED
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new InvalidOrderStateException("Order must be DELIVERED before it can be refunded. Current status: " + order.getStatus());
		}
		
		order.setStatus(OrderStatus.REFUNDED);
		order.setOrderNotes(order.getOrderNotes() != null 
			? order.getOrderNotes() + " | Refunded: " + request.reason() 
			: "Refunded: " + request.reason());
			
		order = orderRepository.save(order);
		log.info("Order {} refunded. Reason: {}", id, request.reason());

		// Outbox Pattern for Refund Event
		try {
			com.demo.order.event.OrderRefundEvent event = com.demo.order.event.OrderRefundEvent.refunded(
					order.getId(), order.getUserId(), request.reason(), request.refundAmount(), request.refundDestination());
			
			OutboxEvent outboxEvent = OutboxEvent.builder()
					.aggregateId(order.getId().toString())
					.aggregateType("Order")
					.eventType(event.eventType())
					.payload(objectMapper.writeValueAsString(event))
					.build();
			outboxEventRepository.save(outboxEvent);
		} catch (Exception e) {
			log.error("Failed to serialize OrderRefundEvent", e);
			throw new RuntimeException("Failed to serialize OrderRefundEvent", e);
		}

		return orderMapper.toResponse(order);
	}

	// ── Helpers ─────────────────────────────────────────────

	private void saveOutboxEvent(OrderEvent event) {
		try {
			OutboxEvent outboxEvent = OutboxEvent.builder().aggregateId(event.orderId().toString())
					.aggregateType("Order").eventType(event.eventType()).payload(objectMapper.writeValueAsString(event))
					.build();
			outboxEventRepository.save(outboxEvent);
		} catch (Exception e) {
			log.error("Failed to serialize OutboxEvent", e);
			throw new RuntimeException("Failed to serialize OutboxEvent", e);
		}
	}

	/** State machine — Java 21 pattern matching switch. */
	private void validateTransition(OrderStatus current, OrderStatus target) {
		boolean allowed = switch (current) {
		case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
		case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
		case PROCESSING -> target == OrderStatus.SHIPPED;
		case SHIPPED -> target == OrderStatus.DELIVERED;
		case DELIVERED -> target == OrderStatus.REFUNDED;
		case CANCELLED, REFUNDED -> false;
		};
		if (!allowed) {
			throw new InvalidOrderStateException("Cannot transition from %s to %s".formatted(current, target));
		}
	}
}

