package com.demo.user.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.demo.common.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * User-service specific exception handler.
 *
 * <p>
 * Takes precedence over {@link com.demo.common.exception.GlobalExceptionHandler}
 * for domain-specific exceptions, returning the correct HTTP status codes instead
 * of the generic 500.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class UserExceptionHandler {

	/**
	 * 409 Conflict — duplicate email on registration.
	 */
	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
		log.warn("Registration attempt with existing email: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("EMAIL_ALREADY_EXISTS", ex.getMessage()));
	}

	/**
	 * 404 Not Found — user lookup by id failed.
	 */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
		log.warn("User not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("USER_NOT_FOUND", ex.getMessage()));
	}

	/**
	 * 404 Not Found — username lookup failed.
	 */
	@ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound(org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
		log.warn("User not found: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("USER_NOT_FOUND", ex.getMessage()));
	}

	/**
	 * 401 Unauthorized — refresh token is expired or invalid.
	 */
	@ExceptionHandler(TokenRefreshException.class)
	public ResponseEntity<ApiResponse<Void>> handleTokenRefresh(TokenRefreshException ex) {
		log.warn("Token refresh failed: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("TOKEN_REFRESH_ERROR", ex.getMessage()));
	}

	/**
	 * 401 Unauthorized — wrong email/password during login.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
		log.warn("Bad credentials: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("INVALID_CREDENTIALS", "Invalid email or password"));
	}

	/**
	 * 401 Unauthorized — account disabled.
	 */
	@ExceptionHandler({DisabledException.class, LockedException.class})
	public ResponseEntity<ApiResponse<Void>> handleAccountStatus(RuntimeException ex) {
		log.warn("Account access denied: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("ACCOUNT_DISABLED", "Your account is disabled or locked"));
	}
}
