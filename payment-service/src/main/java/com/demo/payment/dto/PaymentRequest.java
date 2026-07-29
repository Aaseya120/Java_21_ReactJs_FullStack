package com.demo.payment.dto;

import java.math.BigDecimal;

import com.demo.payment.entity.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payment request DTO — Java 21 record with Jakarta validation and comprehensive industry-standard instrument details.
 */
public record PaymentRequest(
		@NotNull(message = "Order ID is required")
		Long orderId,

		@NotNull(message = "User ID is required")
		Long userId,

		@NotNull(message = "Amount is required")
		@Positive(message = "Amount must be greater than zero")
		BigDecimal amount,

		@NotBlank(message = "Currency is required")
		String currency,

		@NotNull(message = "Payment method is required")
		PaymentMethod paymentMethod,

		@NotBlank(message = "Idempotency key is required")
		String idempotencyKey,

		// General token / test simulation token
		String paymentToken,

		// Card-specific PCI-DSS metadata
		String cardToken,
		String cardLast4,
		String cardBrand,

		// UPI-specific metadata (India Standard)
		String upiVpa,

		// Net Banking specific metadata
		String bankCode,

		// Wallet / BNPL specific metadata
		String walletProvider,

		// Recurring Mandate / Direct Debit reference (ACH, SEPA, UPI AutoPay)
		String mandateReference,

		// Installment / EMI tenure in months
		Integer emiTenureMonths,

		// Target gateway preference
		String gatewayProvider
) {
	// Compact constructor for backward compatibility in simple tests
	public PaymentRequest(Long orderId, Long userId, BigDecimal amount, String currency,
			PaymentMethod paymentMethod, String idempotencyKey, String paymentToken) {
		this(orderId, userId, amount, currency, paymentMethod, idempotencyKey, paymentToken,
				null, null, null, null, null, null, null, null, null);
	}

	// Overload for card/UPI/netbanking without mandate/EMI
	public PaymentRequest(Long orderId, Long userId, BigDecimal amount, String currency,
			PaymentMethod paymentMethod, String idempotencyKey, String paymentToken,
			String cardToken, String cardLast4, String cardBrand, String upiVpa,
			String bankCode, String walletProvider, String gatewayProvider) {
		this(orderId, userId, amount, currency, paymentMethod, idempotencyKey, paymentToken,
				cardToken, cardLast4, cardBrand, upiVpa, bankCode, walletProvider, null, null, gatewayProvider);
	}
}
