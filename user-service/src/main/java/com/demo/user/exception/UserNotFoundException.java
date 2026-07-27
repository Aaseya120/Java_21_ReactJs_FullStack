package com.demo.user.exception;

/**
 * Thrown when a requested user does not exist.
 */
public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException(String identifier) {
		super("User not found: " + identifier);
	}
}
