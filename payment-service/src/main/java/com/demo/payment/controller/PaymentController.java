package com.demo.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.dto.ApiResponse;
import com.demo.payment.dto.PaymentRequest;
import com.demo.payment.dto.PaymentResponse;
import com.demo.payment.dto.RefundRequest;
import com.demo.payment.service.PaymentService;

import com.demo.payment.security.PaymentCryptographyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment REST Controller. Exposes endpoints for processing payments, retrieving
 * payment history, issuing refunds, and retrieving RSA Public Keys for PCI-DSS tokenization.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentController {

	private final PaymentService paymentService;
	private final PaymentCryptographyService paymentCryptographyService;

	@GetMapping("/security/public-key")
	public ResponseEntity<ApiResponse<String>> getPaymentPublicKey() {
		log.info("REST request to get Merchant RSA Public Key PEM for client-side card tokenization");
		return ResponseEntity.ok(ApiResponse.success(paymentCryptographyService.getPublicKeyPem()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
		log.info("REST request to process payment for order ID: {}", request.orderId());
		PaymentResponse response = paymentService.processPayment(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
		log.info("REST request to get payment ID: {}", id);
		PaymentResponse response = paymentService.getPaymentById(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/order/{orderId}")
	public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByOrderId(@PathVariable Long orderId) {
		log.info("REST request to get payments for order ID: {}", orderId);
		List<PaymentResponse> response = paymentService.getPaymentsByOrderId(orderId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByUserId(@PathVariable Long userId) {
		log.info("REST request to get payments for user ID: {}", userId);
		List<PaymentResponse> response = paymentService.getPaymentsByUserId(userId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/{id}/refund")
	public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
			@PathVariable Long id,
			@Valid @RequestBody RefundRequest request) {
		log.info("REST request to refund payment ID: {}", id);
		PaymentResponse response = paymentService.refundPayment(id, request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
