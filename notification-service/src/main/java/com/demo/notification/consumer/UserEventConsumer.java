package com.demo.notification.consumer;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.demo.common.util.KafkaIdempotencyGuard;
import com.demo.notification.dto.NotificationMessage;
import com.demo.notification.dto.NotificationType;
import com.demo.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for user events from the {@code user-events} topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;
	private final KafkaIdempotencyGuard idempotencyGuard;

	@KafkaListener(topics = "user-events", groupId = "${spring.kafka.consumer.group-id}")
	public void consumeUserEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {

		String uniqueEventId = "user-" + record.key() + "-" + record.offset();
		if (idempotencyGuard.isDuplicate(uniqueEventId)) {
			log.info("Skipping duplicate user event for key={} at offset={}", record.key(), record.offset());
			ack.acknowledge();
			return;
		}

		try {
			Map<String, Object> payload = objectMapper.readValue(record.value(), new TypeReference<>() {
			});
			String eventType = extractString(payload, "eventType");
			String userId = extractString(payload, "userId");
			String email = extractString(payload, "email");

			log.info("Received user event: type={}, userId={}", eventType, userId);

			// Java 21 pattern matching switch for user event routing
			NotificationMessage notification = switch (eventType) {
			case "USER_REGISTERED" -> NotificationMessage.of(userId, email, "Welcome to Microservices Demo! 👋", """
					Hi there!

					Welcome to Microservices Demo. Your account has been created successfully.

					Start shopping now and enjoy great deals!
					""", NotificationType.EMAIL);
			default -> {
				log.debug("Ignoring unhandled user event type: {}", eventType);
				yield null;
			}
			};

			if (notification != null) {
				notificationService.dispatch(notification);
			}
			ack.acknowledge();
		} catch (Exception ex) {
			log.error("Failed to process user event: {}", ex.getMessage(), ex);
			// Throw exception to trigger Kafka Retry and DLQ mechanisms
			throw new RuntimeException("Unexpected error processing user event", ex);
		}
	}

	private String extractString(Map<String, Object> map, String key) {
		Object val = map.get(key);
		return val != null ? val.toString() : "UNKNOWN";
	}
}
