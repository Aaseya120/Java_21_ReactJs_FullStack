package com.demo.payment.consumer;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.demo.payment.dto.PaymentResponse;
import com.demo.payment.dto.RefundRequest;
import com.demo.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

	private final PaymentService paymentService;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "order-events", groupId = "payment-service-order-group")
	public void consumeOrderEvent(@Payload String message) {
		log.info("Received order event: {}", message);
		
		try {
			JsonNode jsonNode = objectMapper.readTree(message);
			String eventType = jsonNode.path("eventType").asText();
			
			if ("ORDER_REFUNDED".equals(eventType)) {
				Long orderId = jsonNode.path("orderId").asLong();
				String reason = jsonNode.path("reason").asText("Admin requested proactive refund");
				
				BigDecimal refundAmount = null;
				if (jsonNode.hasNonNull("refundAmount")) {
					refundAmount = new BigDecimal(jsonNode.path("refundAmount").asText());
				}
				
				log.info("Processing ORDER_REFUNDED for orderId: {}", orderId);
				
				// Fetch the payment for this order
				List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId);
				if (payments == null || payments.isEmpty()) {
					log.warn("No payment found for orderId: {}. Cannot process refund.", orderId);
					return;
				}
				
				// Assuming one primary payment per order for simplicity
				PaymentResponse primaryPayment = payments.get(0);
				
				if (com.demo.payment.entity.PaymentStatus.SUCCESS.equals(primaryPayment.status())) {
					RefundRequest refundRequest = new RefundRequest(reason, refundAmount);
					paymentService.refundPayment(primaryPayment.id(), refundRequest);
					log.info("Successfully initiated refund for paymentId: {} for orderId: {}", primaryPayment.id(), orderId);
				} else {
					log.info("Payment {} for order {} is not in SUCCESS state (current: {}), skipping refund.", 
							primaryPayment.id(), orderId, primaryPayment.status());
				}
			}
		} catch (JsonProcessingException e) {
			log.error("Failed to parse order event message", e);
		} catch (Exception e) {
			log.error("Error processing order event", e);
		}
	}
}
