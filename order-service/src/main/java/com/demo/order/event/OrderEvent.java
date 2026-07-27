package com.demo.order.event;

import java.time.Instant;

/**
 * Kafka event published when an order changes state. Java 21 record for
 * immutable, serialization-friendly event payload.
 */
public record OrderEvent(Long orderId, String eventType, // ORDER_CREATED, ORDER_CONFIRMED, ORDER_CANCELLED, etc.
		Long userId, Long productId, Integer quantity, String status, Instant occurredAt) {
	/** Factory for created events. */
	public static OrderEvent created(Long orderId, Long userId, Long productId, Integer quantity) {
		return new OrderEvent(orderId, "ORDER_CREATED", userId, productId, quantity, "PENDING", Instant.now());
	}

	/** Factory for status-change events. */
	public static OrderEvent statusChanged(Long orderId, Long userId, String newStatus) {
		return new OrderEvent(orderId, "ORDER_STATUS_CHANGED", userId, null, null, newStatus, Instant.now());
	}

	/** Factory for cancelled events. */
	public static OrderEvent cancelled(Long orderId, Long userId) {
		return new OrderEvent(orderId, "ORDER_CANCELLED", userId, null, null, "CANCELLED", Instant.now());
	}
}

