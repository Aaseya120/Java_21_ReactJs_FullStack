package com.demo.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Global API Response Message Constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageConstants {

	// Success Messages
	public static final String MSG_SUCCESS_RETRIEVE = "Retrieved successfully";
	public static final String MSG_SUCCESS_CREATE = "Created successfully";
	public static final String MSG_SUCCESS_UPDATE = "Updated successfully";
	public static final String MSG_SUCCESS_DELETE = "Deleted successfully";

	// Order Specific
	public static final String MSG_ORDER_CREATED = "Order created successfully";
	public static final String MSG_ORDER_RETRIEVED = "Order retrieved successfully";
	public static final String MSG_ORDERS_RETRIEVED = "Orders retrieved successfully";

	// Product Specific
	public static final String MSG_PRODUCT_CREATED = "Product created successfully";
	public static final String MSG_PRODUCT_UPDATED = "Product updated successfully";
	public static final String MSG_PRODUCT_DELETED = "Product deleted successfully";
	public static final String MSG_PRODUCT_RETRIEVED = "Product retrieved successfully";
	public static final String MSG_PRODUCTS_RETRIEVED = "Products retrieved successfully";
	public static final String MSG_STOCK_DEDUCTED = "Stock deducted successfully";
	public static final String MSG_STOCK_ADDED = "Stock added successfully";

	// Error Messages
	public static final String MSG_ERR_DUPLICATE_REQ = "Duplicate request detected. Request with this Idempotency-Key is already processed.";
	public static final String MSG_ERR_INTERNAL_SERVER = "An unexpected error occurred";

	// User/Auth Specific
	public static final String MSG_USER_REGISTERED = "User registered successfully";
	public static final String MSG_LOGIN_SUCCESS = "Login successful";
	public static final String MSG_USER_RETRIEVED = "User profile retrieved successfully";
	public static final String MSG_USER_UPDATED = "User profile updated successfully";
}
