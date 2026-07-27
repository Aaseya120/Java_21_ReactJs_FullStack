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

	@KafkaListener(topics = KafkaConstants.TOPIC_ORDER_EVENTS, groupId = "product-saga-group")
	public void onOrderEvent(String payload) {
		try {
			JsonNode event = objectMapper.readTree(payload);
			String eventType = event.get("eventType").asText();

			if ("ORDER_CREATED".equals(eventType)) {
				Long orderId = Long.parseLong(event.get("orderId").asText());
				Long productId = Long.parseLong(event.get("productId").asText());
				int quantity = event.get("quantity").asInt();

				log.info("SAGA: Processing order {} for inventory reservation", orderId);

				try {
					productCommandService.deductStock(productId, quantity);

					// Success -> Emit RESERVED
					emitInventoryEvent(orderId, "RESERVED");

				} catch (Exception ex) {
					log.error("SAGA: Inventory reservation failed for order {}: {}", orderId, ex.getMessage());

					// Failure -> Emit FAILED
					emitInventoryEvent(orderId, "FAILED");
				}
			}
		} catch (Exception e) {
			log.error("Error processing order event in saga: {}", e.getMessage());
		}
	}

	private void emitInventoryEvent(Long orderId, String status) {
		String eventJson = String.format("{\"orderId\":\"%s\", \"status\":\"%s\"}", orderId, status);
		kafkaTemplate.send(KafkaConstants.TOPIC_INVENTORY_EVENTS, orderId.toString(), eventJson);
	}
}

