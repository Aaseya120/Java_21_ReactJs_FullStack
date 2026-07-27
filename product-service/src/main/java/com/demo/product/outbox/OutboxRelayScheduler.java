package com.demo.product.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Outbox Relay Scheduler for Product Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private static final String PRODUCT_EVENTS_TOPIC = "product-events";

	// Poll every 5 seconds
	@Scheduled(fixedDelayString = "5000")
	@Transactional
	public void processOutboxEvents() {
		List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

		if (!events.isEmpty()) {
			log.info("Found {} unprocessed outbox events", events.size());
		}

		for (OutboxEvent event : events) {
			try {
				// Synchronous send — blocks until Kafka confirms receipt
				kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, event.getAggregateId(), event.getPayload()).get();
				event.setProcessed(true);
				outboxEventRepository.save(event);
				log.debug("Successfully published outbox event {}", event.getId());
			} catch (Exception e) {
				log.error("Failed to publish outbox event {}. Stopping relay to maintain ordering.", event.getId(), e);
				break; // Stop processing to maintain event ordering
			}
		}
	}
}
