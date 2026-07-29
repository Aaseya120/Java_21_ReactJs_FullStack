package com.demo.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/**
 * Refund request DTO — Java 21 record.
 */
public record RefundRequest(
		@NotBlank(message = "Refund reason is required")
		String reason,

		BigDecimal refundAmount
) {
}
