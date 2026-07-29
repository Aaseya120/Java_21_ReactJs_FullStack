package com.demo.order.saga;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.demo.common.constant.SagaInventoryStatus;

import com.demo.common.constant.KafkaConstants;
import com.demo.order.entity.OrderStatus;
import com.demo.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Choreography Saga Listener. Listens for inventory events to finalize the
 * distributed transaction (Saga).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrderListener {

	private final OrderService orderService;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = KafkaConstants.TOPIC_INVENTORY_EVENTS, groupId = "order-saga-group")
	public void onInventoryEvent(String payload) {
		try {
			JsonNode event = objectMapper.readTree(payload);
			Long orderId = Long.parseLong(event.get("orderId").asText());
			String status = event.get("status").asText();

			log.info("SAGA: Received inventory response for order {}: {}", orderId, status);

			if (SagaInventoryStatus.RESERVED.name().equals(status)) {
				orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
				log.info("SAGA: Order {} confirmed.", orderId);
			} else if (SagaInventoryStatus.FAILED.name().equals(status)) {
				orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
				log.warn("SAGA: Order {} cancelled due to inventory failure (Compensation).", orderId);
			}

		} catch (Exception e) {
			log.error("Error processing inventory event in saga: {}", e.getMessage());
			// Throw exception to trigger Kafka Retry and DLQ mechanisms
			throw new RuntimeException("Unexpected error processing inventory event", e);
		}
	}

	@KafkaListener(topics = KafkaConstants.TOPIC_PAYMENT_EVENTS, groupId = "order-saga-group")
	public void onPaymentEvent(String payload) {
		try {
			JsonNode event = objectMapper.readTree(payload);
			Long orderId = event.get("orderId").asLong();
			String status = event.get("status").asText();

			log.info("SAGA: Received payment response for order {}: {}", orderId, status);

			if ("SUCCESS".equals(status)) {
				// Assuming order service considers it CONFIRMED (or PROCESSING) once paid
				orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
				log.info("SAGA: Order {} confirmed via successful payment.", orderId);
			} else if ("FAILED".equals(status)) {
				// Payment failed, we could optionally cancel the order if it wasn't COD
				log.warn("SAGA: Payment failed for order {}. Manual intervention may be needed.", orderId);
			}

		} catch (Exception e) {
			log.error("Error processing payment event in saga: {}", e.getMessage());
			throw new RuntimeException("Unexpected error processing payment event", e);
		}
	}
}

