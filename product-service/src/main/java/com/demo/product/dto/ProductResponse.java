package com.demo.product.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Product response DTO — Java 21 record with factory method.
 *
 * <p>
 * Demonstrates Java 21 pattern matching in the availability description.
 */
public record ProductResponse(Long id, String name, String description, BigDecimal price, int stockQty, String category,
		String sku, String imageUrl, boolean active, String availabilityStatus, OffsetDateTime createdAt) {
}

