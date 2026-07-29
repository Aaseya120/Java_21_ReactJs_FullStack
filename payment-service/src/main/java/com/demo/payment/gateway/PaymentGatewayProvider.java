package com.demo.payment.gateway;

import java.math.BigDecimal;

import com.demo.payment.dto.PaymentRequest;

/**
 * Payment gateway provider interface. Pluggable design allowing Stripe, PayPal,
 * Razorpay, or custom banking simulators.
 */
public interface PaymentGatewayProvider {

	GatewayChargeResult charge(PaymentRequest request);

	GatewayRefundResult refund(String transactionReference, BigDecimal amount);
}
