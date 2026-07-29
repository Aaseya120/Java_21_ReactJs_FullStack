package com.demo.payment.model;

/**
 * Java 21 Sealed Interface modeling gateway execution outcomes.
 * 
 * Permitted records enable compile-time exhaustive switch checks without default fallbacks.
 */
public sealed interface GatewayExecutionResult permits
		GatewayExecutionResult.Success,
		GatewayExecutionResult.Failure,
		GatewayExecutionResult.PendingAction {

	record Success(
			String transactionReference,
			String gatewayProvider,
			String authorizationCode
	) implements GatewayExecutionResult {}

	record Failure(
			String errorCode,
			String errorMessage,
			boolean isRetryable
	) implements GatewayExecutionResult {}

	record PendingAction(
			String redirectUrl,
			String actionType,
			String expiresAt
	) implements GatewayExecutionResult {}
}
