package com.demo.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.demo.common.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Product-service specific exception handler.
 *
 * <p>
 * Takes precedence over {@link com.demo.common.exception.GlobalExceptionHandler}
 * for domain-specific exceptions, returning the correct HTTP status codes instead
 * of the generic 500.
 */
@RestControllerAdvice
@Slf4j
public class ProductExceptionHandler {

	/**
	 * 404 Not Found — product lookup by id failed.
	 */
	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
		log.warn("Product not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("PRODUCT_NOT_FOUND", ex.getMessage()));
	}

	/**
	 * 400 Bad Request — insufficient stock for order.
	 */
	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
		log.warn("Insufficient stock: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("INSUFFICIENT_STOCK", ex.getMessage()));
	}
}
