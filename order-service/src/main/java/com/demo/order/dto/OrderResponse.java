package com.demo.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.demo.order.entity.OrderStatus;

/**
 * Order response DTO — Java 21 record.
 */
public record OrderResponse(Long id, Long userId, Long productId, int quantity, BigDecimal totalPrice,
		OrderStatus status, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt,
		/** Human-readable status description via pattern matching switch */
		String statusDescription) {
}

