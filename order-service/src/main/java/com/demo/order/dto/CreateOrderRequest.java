package com.demo.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Order creation request — Java 21 record with Bean Validation.
 */
public record CreateOrderRequest(

		@NotNull(message = "User ID is required") Long userId,

		@NotNull(message = "Product ID is required") Long productId,

		@Min(value = 1, message = "Quantity must be at least 1") @Max(value = 1000, message = "Quantity cannot exceed 1000") int quantity,

		@NotNull(message = "Total price is required") @DecimalMin(value = "0.01", message = "Total price must be greater than 0") BigDecimal totalPrice,

		String notes) {
}

