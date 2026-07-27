package com.demo.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Global Kafka Topic Constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaConstants {
	public static final String TOPIC_USER_EVENTS = "user-events";
	public static final String TOPIC_ORDER_EVENTS = "order-events";
	public static final String TOPIC_INVENTORY_EVENTS = "inventory-events";
}
