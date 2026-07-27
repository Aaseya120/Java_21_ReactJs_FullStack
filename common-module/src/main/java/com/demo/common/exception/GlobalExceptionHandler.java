package com.demo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.demo.common.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Global Exception Handler for all microservices. Formats errors using standard
 * ApiResponse.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
		log.warn("Illegal argument: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
	}

	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
		log.warn("Type mismatch: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", "Invalid parameter format: " + ex.getValue()));
	}

	@ExceptionHandler({java.util.NoSuchElementException.class, org.springframework.web.servlet.resource.NoResourceFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex, WebRequest request) {
		log.error("Internal server error: ", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
	}

	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("CONFLICT", "Duplicate entry or data integrity violation"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).reduce((a, b) -> a + ", " + b)
				.orElse("Validation error");
		log.warn("Validation failed: {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
	public void handleAsyncRequestNotUsable(org.springframework.web.context.request.async.AsyncRequestNotUsableException ex) {
		log.info("Client disconnected from async stream: {}", ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
		if (ex.getMessage() != null && ex.getMessage().contains("Cannot start async")) {
			log.info("Client disconnected before async started.");
			return null;
		}
		log.error("Illegal state error: ", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("INTERNAL_SERVER_ERROR", ex.getMessage()));
	}
}
