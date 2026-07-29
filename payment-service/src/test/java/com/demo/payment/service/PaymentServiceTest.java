package com.demo.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.payment.dto.PaymentRequest;
import com.demo.payment.dto.PaymentResponse;
import com.demo.payment.dto.RefundRequest;
import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentMethod;
import com.demo.payment.entity.PaymentStatus;
import com.demo.payment.gateway.GatewayChargeResult;
import com.demo.payment.gateway.PaymentGatewayProvider;
import com.demo.payment.mapper.PaymentMapper;
import com.demo.payment.outbox.PaymentOutboxRepository;
import com.demo.payment.repository.PaymentAuditLogRepository;
import com.demo.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private PaymentAuditLogRepository auditLogRepository;
	@Mock
	private PaymentOutboxRepository outboxRepository;
	@Mock
	private PaymentGatewayProvider gatewayProvider;
	@Mock
	private PaymentMapper paymentMapper;
	@Mock
	private ObjectMapper objectMapper;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(
				paymentRepository,
				auditLogRepository,
				outboxRepository,
				gatewayProvider,
				paymentMapper,
				objectMapper
		);
	}

	@Test
	void testProcessPayment_IdempotencyMatch() {
		PaymentRequest request = new PaymentRequest(
				100L, 200L, BigDecimal.valueOf(99.99), "USD",
				PaymentMethod.CARD, "IDEM-12345", null
		);

		Payment existingPayment = Payment.builder()
				.id("1")
				.orderId(100L)
				.userId(200L)
				.amount(BigDecimal.valueOf(99.99))
				.currency("USD")
				.status(PaymentStatus.SUCCESS)
				.idempotencyKey("IDEM-12345")
				.build();

		PaymentResponse responseDto = new PaymentResponse(
				"1", 100L, 200L, BigDecimal.valueOf(99.99), "USD",
				PaymentStatus.SUCCESS, "Payment completed successfully", PaymentMethod.CARD,
				"TX-123", "IDEM-12345", null, null, null
		);

		when(paymentRepository.findByIdempotencyKey("IDEM-12345")).thenReturn(Optional.of(existingPayment));
		when(paymentMapper.toResponse(existingPayment)).thenReturn(responseDto);

		PaymentResponse actual = paymentService.processPayment(request);

		assertNotNull(actual);
		assertEquals(PaymentStatus.SUCCESS, actual.status());
		verify(gatewayProvider, never()).charge(any());
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void testProcessPayment_SuccessfulCardCharge() throws Exception {
		PaymentRequest request = new PaymentRequest(
				100L, 200L, BigDecimal.valueOf(99.99), "USD",
				PaymentMethod.CREDIT_CARD, "IDEM-CC-001", null,
				"tok_visa_4242", "4242", "VISA", null, null, null, "STRIPE_SIMULATOR"
		);

		when(paymentRepository.findByIdempotencyKey("IDEM-CC-001")).thenReturn(Optional.empty());

		Payment savedPending = Payment.builder()
				.id("1")
				.orderId(100L)
				.userId(200L)
				.amount(BigDecimal.valueOf(99.99))
				.currency("USD")
				.status(PaymentStatus.PENDING)
				.paymentMethod(PaymentMethod.CREDIT_CARD)
				.idempotencyKey("IDEM-CC-001")
				.cardLast4("4242")
				.cardBrand("VISA")
				.build();

		Payment savedSuccess = Payment.builder()
				.id("1")
				.orderId(100L)
				.userId(200L)
				.amount(BigDecimal.valueOf(99.99))
				.currency("USD")
				.status(PaymentStatus.SUCCESS)
				.paymentMethod(PaymentMethod.CREDIT_CARD)
				.idempotencyKey("IDEM-CC-001")
				.transactionReference("CARD-TX-123456")
				.cardLast4("4242")
				.cardBrand("VISA")
				.build();

		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPending, savedSuccess);
		when(gatewayProvider.charge(request)).thenReturn(GatewayChargeResult.successful("CARD-TX-123456"));
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		PaymentResponse responseDto = new PaymentResponse(
				"1", 100L, 200L, BigDecimal.valueOf(99.99), "USD",
				PaymentStatus.SUCCESS, "Payment completed successfully", PaymentMethod.CREDIT_CARD,
				"CARD-TX-123456", "IDEM-CC-001", null, "4242", "VISA", null, null, null, "STRIPE_SIMULATOR", null, null
		);
		when(paymentMapper.toResponse(savedSuccess)).thenReturn(responseDto);

		PaymentResponse actual = paymentService.processPayment(request);

		assertNotNull(actual);
		assertEquals(PaymentStatus.SUCCESS, actual.status());
		assertEquals("VISA", actual.cardBrand());
		assertEquals("CARD-TX-123456", actual.transactionReference());
		verify(gatewayProvider, times(1)).charge(request);
		verify(auditLogRepository, times(2)).save(any());
		verify(outboxRepository, times(1)).save(any());
	}

	@Test
	void testProcessPayment_SuccessfulUpiCharge() throws Exception {
		PaymentRequest request = new PaymentRequest(
				101L, 201L, BigDecimal.valueOf(150.00), "INR",
				PaymentMethod.UPI, "IDEM-UPI-001", null,
				null, null, null, "customer@okicici", null, null, "RAZORPAY_SIMULATOR"
		);

		when(paymentRepository.findByIdempotencyKey("IDEM-UPI-001")).thenReturn(Optional.empty());

		Payment savedPending = Payment.builder()
				.id("2")
				.orderId(101L)
				.userId(201L)
				.amount(BigDecimal.valueOf(150.00))
				.currency("INR")
				.status(PaymentStatus.PENDING)
				.paymentMethod(PaymentMethod.UPI)
				.idempotencyKey("IDEM-UPI-001")
				.upiVpa("customer@okicici")
				.build();

		Payment savedSuccess = Payment.builder()
				.id("2")
				.orderId(101L)
				.userId(201L)
				.amount(BigDecimal.valueOf(150.00))
				.currency("INR")
				.status(PaymentStatus.SUCCESS)
				.paymentMethod(PaymentMethod.UPI)
				.idempotencyKey("IDEM-UPI-001")
				.upiVpa("customer@okicici")
				.transactionReference("UPI-UTR-987654321")
				.build();

		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPending, savedSuccess);
		when(gatewayProvider.charge(request)).thenReturn(GatewayChargeResult.successful("UPI-UTR-987654321"));
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		PaymentResponse responseDto = new PaymentResponse(
				"2", 101L, 201L, BigDecimal.valueOf(150.00), "INR",
				PaymentStatus.SUCCESS, "Payment completed successfully", PaymentMethod.UPI,
				"UPI-UTR-987654321", "IDEM-UPI-001", null, null, null, "customer@okicici", null, null, "RAZORPAY_SIMULATOR", null, null
		);
		when(paymentMapper.toResponse(savedSuccess)).thenReturn(responseDto);

		PaymentResponse actual = paymentService.processPayment(request);

		assertNotNull(actual);
		assertEquals(PaymentStatus.SUCCESS, actual.status());
		assertEquals("customer@okicici", actual.upiVpa());
		assertEquals("UPI-UTR-987654321", actual.transactionReference());
	}

	@Test
	void testProcessPayment_SuccessfulNetBankingCharge() throws Exception {
		PaymentRequest request = new PaymentRequest(
				102L, 202L, BigDecimal.valueOf(500.00), "INR",
				PaymentMethod.NET_BANKING, "IDEM-NB-001", null,
				null, null, null, null, "HDFC_BANK", null, "BILLDESK_SIMULATOR"
		);

		when(paymentRepository.findByIdempotencyKey("IDEM-NB-001")).thenReturn(Optional.empty());

		Payment savedPending = Payment.builder()
				.id("3")
				.orderId(102L)
				.userId(202L)
				.amount(BigDecimal.valueOf(500.00))
				.currency("INR")
				.status(PaymentStatus.PENDING)
				.paymentMethod(PaymentMethod.NET_BANKING)
				.idempotencyKey("IDEM-NB-001")
				.bankCode("HDFC_BANK")
				.build();

		Payment savedSuccess = Payment.builder()
				.id("3")
				.orderId(102L)
				.userId(202L)
				.amount(BigDecimal.valueOf(500.00))
				.currency("INR")
				.status(PaymentStatus.SUCCESS)
				.paymentMethod(PaymentMethod.NET_BANKING)
				.idempotencyKey("IDEM-NB-001")
				.bankCode("HDFC_BANK")
				.transactionReference("NB-REF-55667788")
				.build();

		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPending, savedSuccess);
		when(gatewayProvider.charge(request)).thenReturn(GatewayChargeResult.successful("NB-REF-55667788"));
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		PaymentResponse responseDto = new PaymentResponse(
				"3", 102L, 202L, BigDecimal.valueOf(500.00), "INR",
				PaymentStatus.SUCCESS, "Payment completed successfully", PaymentMethod.NET_BANKING,
				"NB-REF-55667788", "IDEM-NB-001", null, null, null, null, "HDFC_BANK", null, "BILLDESK_SIMULATOR", null, null
		);
		when(paymentMapper.toResponse(savedSuccess)).thenReturn(responseDto);

		PaymentResponse actual = paymentService.processPayment(request);

		assertNotNull(actual);
		assertEquals(PaymentStatus.SUCCESS, actual.status());
		assertEquals("HDFC_BANK", actual.bankCode());
		assertEquals("NB-REF-55667788", actual.transactionReference());
	}

	@Test
	void testRefundPayment_NonSuccessPayment_ThrowsException() {
		Payment pendingPayment = Payment.builder()
				.id("1")
				.status(PaymentStatus.PENDING)
				.build();

		when(paymentRepository.findById("1")).thenReturn(Optional.of(pendingPayment));

		RefundRequest request = new RefundRequest("Customer requested", BigDecimal.valueOf(50.00));

		assertThrows(IllegalStateException.class, () -> paymentService.refundPayment("1", request));
	}
}
