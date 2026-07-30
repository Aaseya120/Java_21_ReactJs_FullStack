package com.demo.order.entity;

/**
 * Order lifecycle status.
 *
 * <p>
 * Used with Java 21 pattern matching switch to route business logic based on
 * the current order state.
 */
public enum OrderStatus {
	PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUND_REQUESTED, REFUND_REJECTED, REFUNDED
}
