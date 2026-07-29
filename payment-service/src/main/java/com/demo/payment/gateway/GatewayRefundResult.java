package com.demo.payment.gateway;

/**
 * Result from payment gateway provider refunding a payment.
 */
public record GatewayRefundResult(
		boolean success,
		String refundReference,
		String errorMessage
) {
	public static GatewayRefundResult successful(String refundRef) {
		return new GatewayRefundResult(true, refundRef, null);
	}

	public static GatewayRefundResult failed(String errorMessage) {
		return new GatewayRefundResult(false, null, errorMessage);
	}
}
