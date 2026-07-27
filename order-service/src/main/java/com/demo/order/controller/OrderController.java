package com.demo.order.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.constant.ApiConstants;
import com.demo.common.constant.MessageConstants;
import com.demo.common.dto.ApiResponse;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.OrderResponse;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for order management. All endpoints require a valid JWT
 * (enforced by API Gateway).
 */
@RestController
@RequestMapping(ApiConstants.OrderApi.BASE)
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

	/**
	 * POST /api/v1/orders — Create a new order with Idempotency.
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
			@RequestHeader(value = ApiConstants.HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
			@Valid @RequestBody CreateOrderRequest request) {

		if (idempotencyKey != null) {
			String redisKey = "idempotency:order:" + idempotencyKey;
			Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING",
					java.time.Duration.ofHours(24));
			if (Boolean.FALSE.equals(isNew)) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(ApiResponse.error("CONFLICT", MessageConstants.MSG_ERR_DUPLICATE_REQ));
			}
		}

		OrderResponse order = orderService.createOrder(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
	}

	/**
	 * GET /api/orders/{id} — Get order by ID (cached in Redis).
	 */
	@GetMapping(ApiConstants.OrderApi.ID)
	public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id,
			@RequestHeader(value = "X-Auth-UserId", required = false) String authUserId,
			@RequestHeader(value = "X-Auth-Role", required = false) String authRole) {

		OrderResponse order = orderService.getOrderById(id);
		if (!"ADMIN".equalsIgnoreCase(authRole) && authUserId != null && !authUserId.isBlank()) {
			try {
				Long loggedInId = Long.valueOf(authUserId);
				if (!loggedInId.equals(order.userId())) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body(ApiResponse.error("FORBIDDEN", "Not authorized to access orders of other users"));
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return ResponseEntity.ok(ApiResponse.success(order));
	}

	/**
	 * GET /api/v1/orders — Get orders scoped to login user (Admin sees all).
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<?>> getAllOrders(
			@RequestHeader(value = "X-Auth-UserId", required = false) String authUserId,
			@RequestHeader(value = "X-Auth-Role", required = false) String authRole,
			@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

		if (!"ADMIN".equalsIgnoreCase(authRole) && authUserId != null && !authUserId.isBlank()) {
			try {
				Long userId = Long.valueOf(authUserId);
				return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUser(userId)));
			} catch (NumberFormatException ignored) {
			}
		}
		return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
	}

	/**
	 * GET /api/v1/orders/user/{userId} — All orders for a user (IDOR protected).
	 */
	@GetMapping(ApiConstants.OrderApi.USER_ID)
	public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
			@PathVariable Long userId,
			@RequestHeader(value = "X-Auth-UserId", required = false) String authUserId,
			@RequestHeader(value = "X-Auth-Role", required = false) String authRole) {

		if (!"ADMIN".equalsIgnoreCase(authRole) && authUserId != null && !authUserId.isBlank()) {
			try {
				Long loggedInId = Long.valueOf(authUserId);
				if (!loggedInId.equals(userId)) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body(ApiResponse.error("FORBIDDEN", "Not authorized to access orders of other users"));
				}
			} catch (NumberFormatException ignored) {
			}
		}

		return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUser(userId)));
	}

	/**
	 * PUT /api/orders/{id}/status — Update order status (state machine).
	 */
	@PutMapping(ApiConstants.OrderApi.STATUS)
	public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
			@RequestParam OrderStatus status) {
		return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status)));
	}

	/**
	 * DELETE /api/orders/{id} — Cancel an order.
	 */
	@DeleteMapping(ApiConstants.OrderApi.ID)
	public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(id)));
	}
}

