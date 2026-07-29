package com.demo.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.payment.dto.PaymentRequest;
import com.demo.payment.dto.PaymentResponse;
import com.demo.payment.dto.RefundRequest;
import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentAuditLog;
import com.demo.payment.entity.PaymentStatus;
import com.demo.payment.gateway.GatewayChargeResult;
import com.demo.payment.gateway.GatewayRefundResult;
import com.demo.payment.gateway.PaymentGatewayProvider;
import com.demo.payment.mapper.PaymentMapper;
import com.demo.payment.outbox.PaymentOutboxEvent;
import com.demo.payment.outbox.PaymentOutboxRepository;
import com.demo.payment.repository.PaymentAuditLogRepository;
import com.demo.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment Service. Implements idempotency protection, transactional outbox events,
 * audit logging, and Redis caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentAuditLogRepository auditLogRepository;
	private final PaymentOutboxRepository outboxRepository;
	private final PaymentGatewayProvider gatewayProvider;
	private final PaymentMapper paymentMapper;
	private final ObjectMapper objectMapper;

	@Transactional
	public PaymentResponse processPayment(PaymentRequest request) {
		log.info("Processing payment for order {} with idempotencyKey {}", request.orderId(), request.idempotencyKey());

		// 1. Idempotency Check
		Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
		if (existing.isPresent()) {
			log.info("Idempotency match: returning existing payment {} for key {}",
					existing.get().getId(), request.idempotencyKey());
			return paymentMapper.toResponse(existing.get());
		}

		// 2. Create Initial Payment Entity (PENDING)
		Payment payment = Payment.builder()
				.orderId(request.orderId())
				.userId(request.userId())
				.amount(request.amount())
				.currency(request.currency())
				.status(PaymentStatus.PENDING)
				.paymentMethod(request.paymentMethod())
				.idempotencyKey(request.idempotencyKey())
				.cardLast4(request.cardLast4())
				.cardBrand(request.cardBrand())
				.upiVpa(request.upiVpa())
				.bankCode(request.bankCode())
				.walletProvider(request.walletProvider())
				.gatewayProvider(request.gatewayProvider() != null ? request.gatewayProvider() : "SIMULATED_GATEWAY")
				.build();

		payment = paymentRepository.save(payment);
		recordAuditLog(payment.getId(), null, PaymentStatus.PENDING, "Payment initiated");

		// 3. Invoke Payment Gateway Provider
		GatewayChargeResult chargeResult = gatewayProvider.charge(request);

		PaymentStatus oldStatus = payment.getStatus();
		if (chargeResult.success()) {
			payment.setStatus(PaymentStatus.SUCCESS);
			payment.setTransactionReference(chargeResult.transactionReference());
			payment.setErrorMessage(null);
			recordAuditLog(payment.getId(), oldStatus, PaymentStatus.SUCCESS, "Charged successfully");
			recordOutboxEvent(payment, "PAYMENT_SUCCESS");
		} else {
			payment.setStatus(PaymentStatus.FAILED);
			payment.setErrorMessage(chargeResult.errorMessage());
			recordAuditLog(payment.getId(), oldStatus, PaymentStatus.FAILED, chargeResult.errorMessage());
			recordOutboxEvent(payment, "PAYMENT_FAILED");
		}

		payment = paymentRepository.save(payment);
		log.info("Payment {} completed with status {}", payment.getId(), payment.getStatus());

		return paymentMapper.toResponse(payment);
	}

	@Cacheable(value = "payments", key = "#id")
	@Transactional(readOnly = true)
	public PaymentResponse getPaymentById(String id) {
		log.info("Fetching payment from DB: {}", id);
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + id));
		return paymentMapper.toResponse(payment);
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
		return paymentRepository.findByOrderId(orderId).stream()
				.map(paymentMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> getPaymentsByUserId(Long userId) {
		return paymentRepository.findByUserId(userId).stream()
				.map(paymentMapper::toResponse)
				.toList();
	}

	@Transactional
	@CachePut(value = "payments", key = "#paymentId")
	public PaymentResponse refundPayment(String paymentId, RefundRequest request) {
		log.info("Refunding payment {} for reason: {}", paymentId, request.reason());
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

		if (payment.getStatus() != PaymentStatus.SUCCESS) {
			throw new IllegalStateException("Only SUCCESS payments can be refunded. Current status: " + payment.getStatus());
		}

		BigDecimal amountToRefund = request.refundAmount() != null ? request.refundAmount() : payment.getAmount();
		GatewayRefundResult refundResult = gatewayProvider.refund(payment.getTransactionReference(), amountToRefund);

		if (!refundResult.success()) {
			throw new IllegalStateException("Gateway refund failed: " + refundResult.errorMessage());
		}

		PaymentStatus oldStatus = payment.getStatus();
		payment.setStatus(PaymentStatus.REFUNDED);
		recordAuditLog(payment.getId(), oldStatus, PaymentStatus.REFUNDED, request.reason());
		recordOutboxEvent(payment, "PAYMENT_REFUNDED");

		payment = paymentRepository.save(payment);
		return paymentMapper.toResponse(payment);
	}

	private void recordAuditLog(String paymentId, PaymentStatus oldStatus, PaymentStatus newStatus, String reason) {
		PaymentAuditLog auditLog = PaymentAuditLog.builder()
				.paymentId(paymentId)
				.previousStatus(oldStatus)
				.newStatus(newStatus)
				.reason(reason)
				.build();
		auditLogRepository.save(auditLog);
	}

	private void recordOutboxEvent(Payment payment, String eventType) {
		try {
			String payload = objectMapper.writeValueAsString(paymentMapper.toResponse(payment));
			PaymentOutboxEvent event = PaymentOutboxEvent.builder()
					.aggregateId(payment.getId())
					.aggregateType("PAYMENT")
					.eventType(eventType)
					.payload(payload)
					.processed(false)
					.build();
			outboxRepository.save(event);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize payment {} for outbox event", payment.getId(), e);
			throw new RuntimeException("Error writing payment outbox event", e);
		}
	}
}
