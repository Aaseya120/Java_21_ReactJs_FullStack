package com.demo.product.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Product catalog entity.
 */
@Entity
@Table(name = "products", indexes = { @Index(name = "idx_products_category", columnList = "category"),
		@Index(name = "idx_products_sku", columnList = "sku") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	@Builder.Default
	private int stockQty = 0;

	@Column(length = 100)
	private String category;

	@Column(unique = true, length = 100)
	private String sku;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(nullable = false)
	@Builder.Default
	private boolean active = true;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private OffsetDateTime updatedAt;
}

