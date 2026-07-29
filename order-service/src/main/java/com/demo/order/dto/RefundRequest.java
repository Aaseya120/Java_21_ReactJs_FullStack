package com.demo.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RefundRequest(
		@NotBlank(message = "Refund reason is required")
		String reason,
		
		@Positive(message = "Refund amount must be positive if specified")
		BigDecimal refundAmount, // Optional. If null, implies full refund.

		String refundDestination // E.g. "ORIGINAL_PAYMENT_METHOD" or "STORE_CREDIT"
) {}
