package com.demo.payment.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payment JPA entity with industry-standard instrument details (Card, UPI, NetBanking, Wallet, EMI, Mandates).
 */
@Entity
@Table(name = "payments", indexes = {
		@Index(name = "idx_payments_order_id", columnList = "order_id"),
		@Index(name = "idx_payments_user_id", columnList = "user_id"),
		@Index(name = "idx_payments_status", columnList = "status"),
		@Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key"),
		@Index(name = "idx_payments_upi_vpa", columnList = "upi_vpa"),
		@Index(name = "idx_payments_bank_code", columnList = "bank_code"),
		@Index(name = "idx_payments_mandate_reference", columnList = "mandate_reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 10)
	@Builder.Default
	private String currency = "USD";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	@Builder.Default
	private PaymentStatus status = PaymentStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 50)
	private PaymentMethod paymentMethod;

	@Column(name = "transaction_reference", length = 100)
	private String transactionReference;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
	private String idempotencyKey;

	@Column(name = "error_message")
	private String errorMessage;

	// Industry-Standard Payment Instrument Metadata
	@Column(name = "card_last4", length = 4)
	private String cardLast4;

	@Column(name = "card_brand", length = 30)
	private String cardBrand;

	@Column(name = "upi_vpa", length = 100)
	private String upiVpa;

	@Column(name = "bank_code", length = 30)
	private String bankCode;

	@Column(name = "wallet_provider", length = 50)
	private String walletProvider;

	@Column(name = "mandate_reference", length = 100)
	private String mandateReference;

	@Column(name = "emi_tenure_months")
	private Integer emiTenureMonths;

	@Column(name = "gateway_provider", length = 50)
	@Builder.Default
	private String gatewayProvider = "SIMULATED_GATEWAY";

	@CreationTimestamp
	@Column(name = "created_at", updatable = false, nullable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
