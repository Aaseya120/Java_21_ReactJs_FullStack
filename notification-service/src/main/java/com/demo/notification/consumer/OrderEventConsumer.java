package com.demo.notification.consumer;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.demo.common.constant.KafkaConstants;
import com.demo.common.util.KafkaIdempotencyGuard;
import com.demo.notification.dto.NotificationMessage;
import com.demo.notification.dto.NotificationType;
import com.demo.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for order events from the {@code order-events} topic.
 *
 * <p>
 * Demonstrates:
 * <ul>
 * <li>Java 21 pattern matching switch to handle different event types
 * exhaustively</li>
 * <li>Manual acknowledgment for at-least-once delivery semantics</li>
 * <li>Structured logging with contextual fields</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;
	private final KafkaIdempotencyGuard idempotencyGuard;

	/**
	 * Listens on the {@code order-events} topic. The message value is a raw JSON
	 * string (sent by the Outbox Relay), parsed here into a Map for flexibility.
	 */
	@KafkaListener(topics = KafkaConstants.TOPIC_ORDER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
	public void consumeOrderEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {

		String orderId = record.key();

		log.info("Received order event — key={}, partition={}, offset={}", orderId, record.partition(),
				record.offset());

		String uniqueEventId = "order-" + orderId + "-" + record.offset();
		if (idempotencyGuard.isDuplicate(uniqueEventId)) {
			log.info("Skipping duplicate order event for key={} at offset={}", orderId, record.offset());
			ack.acknowledge();
			return;
		}

		try {
			Map<String, Object> payload = objectMapper.readValue(record.value(), new TypeReference<>() {
			});
			processOrderEvent(orderId, payload);
			ack.acknowledge();
		} catch (Exception ex) {
			log.error("Failed to process order event for orderId={}: {}", orderId, ex.getMessage(), ex);
			// Throw exception to trigger Kafka Retry and DLQ mechanisms
			throw new RuntimeException("Unexpected error processing order event", ex);
		}
	}

	/**
	 * Routes the event to the correct notification using Java 21 pattern matching
	 * switch.
	 */
	private void processOrderEvent(String orderId, Map<String, Object> payload) {
		String eventType = extractString(payload, "eventType");
		String userId = extractString(payload, "userId");
		String status = extractString(payload, "status");

		log.debug("Processing event: type={}, orderId={}, userId={}", eventType, orderId, userId);

		// Java 21 pattern matching switch — routes by event type string
		NotificationMessage notification = switch (eventType) {
		case "ORDER_CREATED" -> NotificationMessage.of(userId, "venkat.j2se@gmail.com", "Order Confirmed! 🎉",
				notificationService.buildOrderEmailBody(eventType, orderId, status), NotificationType.EMAIL);
		case "ORDER_STATUS_CHANGED" -> NotificationMessage.of(userId, "venkat.j2se@gmail.com", "Order Update: " + status,
				notificationService.buildOrderEmailBody(eventType, orderId, status), NotificationType.EMAIL);
		case "ORDER_CANCELLED" -> NotificationMessage.of(userId, "venkat.j2se@gmail.com", "Order Cancelled",
				notificationService.buildOrderEmailBody(eventType, orderId, status), NotificationType.EMAIL);
		case "ORDER_REFUNDED" -> NotificationMessage.of(userId, "venkat.j2se@gmail.com", "Order Refunded",
				notificationService.buildOrderEmailBody(eventType, orderId, status), NotificationType.EMAIL);
		default -> {
			log.warn("Unknown order event type: {}", eventType);
			yield null;
		}
		};

		if (notification != null) {
			notificationService.dispatch(notification);
		}
	}

	private String extractString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : "UNKNOWN";
	}
}
