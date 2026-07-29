package com.demo.payment.gateway;

import com.demo.payment.entity.PaymentStatus;

/**
 * Result from payment gateway provider charging a payment.
 */
public record GatewayChargeResult(
		boolean success,
		String transactionReference,
		PaymentStatus status,
		String errorMessage
) {
	public static GatewayChargeResult successful(String txRef) {
		return new GatewayChargeResult(true, txRef, PaymentStatus.SUCCESS, null);
	}

	public static GatewayChargeResult failed(String errorMessage) {
		return new GatewayChargeResult(false, null, PaymentStatus.FAILED, errorMessage);
	}
}
