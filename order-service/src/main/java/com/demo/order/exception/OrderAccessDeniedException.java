package com.demo.order.exception;

/**
 * Thrown when a user attempts to access an order belonging to another user.
 */
public class OrderAccessDeniedException extends RuntimeException {

	public OrderAccessDeniedException(String message) {
		super(message);
	}
}
