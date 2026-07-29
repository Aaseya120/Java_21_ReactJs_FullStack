package com.demo.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.demo.payment.entity.PaymentMethod;
import com.demo.payment.entity.PaymentStatus;

/**
 * Payment response DTO — Java 21 record including instrument metadata.
 */
public record PaymentResponse(
		String id,
		Long orderId,
		Long userId,
		BigDecimal amount,
		String currency,
		PaymentStatus status,
		String statusDescription,
		PaymentMethod paymentMethod,
		String transactionReference,
		String idempotencyKey,
		String errorMessage,
		String cardLast4,
		String cardBrand,
		String upiVpa,
		String bankCode,
		String walletProvider,
		String gatewayProvider,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	// Compatibility constructor for existing basic calls
	public PaymentResponse(String id, Long orderId, Long userId, BigDecimal amount, String currency,
			PaymentStatus status, String statusDescription, PaymentMethod paymentMethod,
			String transactionReference, String idempotencyKey, String errorMessage,
			OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this(id, orderId, userId, amount, currency, status, statusDescription, paymentMethod,
				transactionReference, idempotencyKey, errorMessage, null, null, null, null, null, "SIMULATED_GATEWAY",
				createdAt, updatedAt);
	}
}
