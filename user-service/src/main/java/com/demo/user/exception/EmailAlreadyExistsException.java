package com.demo.user.exception;

/**
 * Thrown during registration when the email is already registered.
 */
public class EmailAlreadyExistsException extends RuntimeException {
	public EmailAlreadyExistsException(String email) {
		super("Email already registered: " + email);
	}
}
