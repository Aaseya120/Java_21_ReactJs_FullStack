package com.demo.product.exception;

public class ProductNotFoundException extends java.util.NoSuchElementException {
	public ProductNotFoundException(String id) {
		super("Product not found: " + id);
	}
}
