package com.demo.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Product creation/update request — Java 21 record.
 */
public record ProductRequest(@NotBlank(message = "Product name is required") @Size(min = 2, max = 255) String name,

		String description,

		@NotNull(message = "Price is required") @DecimalMin(value = "0.00", inclusive = false, message = "Price must be positive") BigDecimal price,

		@Min(value = 0, message = "Stock quantity cannot be negative") int stockQty,

		@Size(max = 100) String category,

		@Size(max = 100) String sku,

		String imageUrl) {
}
