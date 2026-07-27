package com.demo.order.saga;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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

			if ("RESERVED".equals(status)) {
				orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
				log.info("SAGA: Order {} confirmed.", orderId);
			} else if ("FAILED".equals(status)) {
				orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
				log.warn("SAGA: Order {} cancelled due to inventory failure (Compensation).", orderId);
			}

		} catch (Exception e) {
			log.error("Error processing inventory event in saga: {}", e.getMessage());
		}
	}
}

