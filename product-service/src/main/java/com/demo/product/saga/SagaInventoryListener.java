package com.demo.product.saga;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.demo.common.constant.KafkaConstants;
import com.demo.product.service.ProductCommandService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Choreography Saga Listener. Listens for new orders and attempts to reserve
 * inventory. Emits success or failure events back to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaInventoryListener {

	private final ProductCommandService productCommandService;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	private final org.redisson.api.RedissonClient redissonClient;
	private static final String IDEMPOTENCY_PREFIX = "idempotency:saga:order:";

	@KafkaListener(topics = KafkaConstants.TOPIC_ORDER_EVENTS, groupId = "product-saga-group")
	public void onOrderEvent(String payload) {
		try {
			JsonNode event = objectMapper.readTree(payload);
			String eventType = event.get("eventType").asText();

			if ("ORDER_CREATED".equals(eventType)) {
				Long orderId = Long.parseLong(event.get("orderId").asText());
				Long productId = Long.parseLong(event.get("productId").asText());
				int quantity = event.get("quantity").asInt();

				// 1. Idempotency Check using Redis
				String idempotencyKey = IDEMPOTENCY_PREFIX + orderId;
				if (!redissonClient.getBucket(idempotencyKey).setIfAbsent("PROCESSED", java.time.Duration.ofDays(1))) {
					log.info("SAGA: Order {} was already processed. Skipping.", orderId);
					return; // Already processed
				}

				log.info("SAGA: Processing order {} for inventory reservation", orderId);

				try {
					// 2. Safe Dual-Write: Deduct stock and save Outbox reply in ONE transaction
					productCommandService.deductStockForSaga(productId, quantity, orderId);

				} catch (com.demo.product.exception.InsufficientStockException | com.demo.product.exception.ProductNotFoundException ex) {
					log.error("SAGA: Inventory reservation failed for order {}: {}", orderId, ex.getMessage());

					// 3. Fallback: If it fails business logic, emit FAILED outbox event
					String eventJson = String.format("{\"orderId\":\"%s\", \"status\":\"FAILED\"}", orderId);
					kafkaTemplate.send(KafkaConstants.TOPIC_INVENTORY_EVENTS, orderId.toString(), eventJson);
				}
			}
		} catch (Exception e) {
			log.error("Error processing order event in saga: {}", e.getMessage());
			// 4. Proper Error Handling: Throw exception to trigger Kafka Retry and DLQ mechanisms
			throw new RuntimeException("Unexpected error processing order event", e);
		}
	}
}

