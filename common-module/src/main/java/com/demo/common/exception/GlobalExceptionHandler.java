package com.demo.common.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import java.util.NoSuchElementException;

import com.demo.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global Exception Handler for all microservices. Formats errors using standard
 * ApiResponse.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

	private final MessageSource messageSource;

	private String getMessage(String code, Object... args) {
		try {
			return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
		} catch (Exception e) {
			return code;
		}
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
		log.warn("Illegal argument: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", getMessage("error.bad_request")));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.warn("Type mismatch: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("BAD_REQUEST", getMessage("error.bad_request")));
	}

	@ExceptionHandler({NoSuchElementException.class, NoResourceFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("NOT_FOUND", getMessage("error.not_found")));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex, WebRequest request) {
		log.error("Internal server error: ", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("INTERNAL_SERVER_ERROR", getMessage("error.internal_server_error")));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("CONFLICT", getMessage("error.conflict")));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String validationMessage = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).reduce((a, b) -> a + ", " + b)
				.orElse("Validation error");
		log.warn("Validation failed: {}", validationMessage);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("VALIDATION_ERROR", getMessage("error.validation", validationMessage)));
	}

	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
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
