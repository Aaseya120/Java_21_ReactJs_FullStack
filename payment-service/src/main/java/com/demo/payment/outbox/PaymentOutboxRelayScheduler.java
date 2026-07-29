package com.demo.payment.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.constant.KafkaConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Outbox Relay Scheduler. Guarantees at-least-once Kafka event delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxRelayScheduler {

	private final PaymentOutboxRepository paymentOutboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Scheduled(fixedDelayString = "5000")
	@Transactional
	public void processOutboxEvents() {
		List<PaymentOutboxEvent> events = paymentOutboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

		if (!events.isEmpty()) {
			log.info("PaymentOutboxRelay: Found {} unprocessed payment outbox events", events.size());
		}

		for (PaymentOutboxEvent event : events) {
			try {
				kafkaTemplate.send(KafkaConstants.TOPIC_PAYMENT_EVENTS, event.getAggregateId(), event.getPayload()).get();
				event.setProcessed(true);
				paymentOutboxRepository.save(event);
				log.debug("PaymentOutboxRelay: Successfully published event {}", event.getId());
			} catch (Exception e) {
				log.error("PaymentOutboxRelay: Failed to publish outbox event {}. Stopping relay to maintain ordering.", event.getId(), e);
				break;
			}
		}
	}
}
