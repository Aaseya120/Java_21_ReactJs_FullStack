package com.demo.order.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderRefundEvent(
		Long orderId,
		String eventType,
		Long userId,
		String reason,
		BigDecimal refundAmount,
		String refundDestination,
		Instant occurredAt
) {
	public static OrderRefundEvent refunded(Long orderId, Long userId, String reason, BigDecimal refundAmount, String refundDestination) {
		return new OrderRefundEvent(orderId, "ORDER_REFUNDED", userId, reason, refundAmount, refundDestination, Instant.now());
	}
}
