package com.demo.order.exception;

public class OrderNotFoundException extends java.util.NoSuchElementException {
	public OrderNotFoundException(String id) {
		super("Order not found: " + id);
	}
}
