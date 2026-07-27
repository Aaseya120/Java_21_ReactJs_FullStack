package com.demo.user.exception;

/**
 * Thrown when a refresh token is expired, invalid, or not found.
 */
public class TokenRefreshException extends RuntimeException {
	public TokenRefreshException(String message) {
		super(message);
	}
}
