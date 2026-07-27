package com.demo.order.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.demo.common.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Order-service specific exception handler.
 *
 * <p>
 * Takes precedence over {@link com.demo.common.exception.GlobalExceptionHandler}
 * for domain-specific exceptions, returning the correct HTTP status codes instead
 * of the generic 500.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class OrderExceptionHandler {

	/**
	 * 404 Not Found — order lookup by id failed.
	 */
	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
		log.warn("Order not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("ORDER_NOT_FOUND", ex.getMessage()));
	}

	/**
	 * 400 Bad Request — invalid order state transition.
	 */
	@ExceptionHandler(InvalidOrderStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidOrderState(InvalidOrderStateException ex) {
		log.warn("Invalid order state: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("INVALID_ORDER_STATE", ex.getMessage()));
	}

	/**
	 * 403 Forbidden — user attempted to access another user's order.
	 */
	@ExceptionHandler(OrderAccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleOrderAccessDenied(OrderAccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error("FORBIDDEN", ex.getMessage()));
	}
}
