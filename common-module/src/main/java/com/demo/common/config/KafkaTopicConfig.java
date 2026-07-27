package com.demo.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.demo.common.constant.KafkaConstants;

/**
 * Centralized Kafka topic auto-creation. Safely creates topics idempotently
 * across all microservices.
 */
@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic userEventsTopic() {
		return TopicBuilder.name(KafkaConstants.TOPIC_USER_EVENTS).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic orderEventsTopic() {
		return TopicBuilder.name(KafkaConstants.TOPIC_ORDER_EVENTS).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic inventoryEventsTopic() {
		return TopicBuilder.name(KafkaConstants.TOPIC_INVENTORY_EVENTS).partitions(3).replicas(1).build();
	}
}
