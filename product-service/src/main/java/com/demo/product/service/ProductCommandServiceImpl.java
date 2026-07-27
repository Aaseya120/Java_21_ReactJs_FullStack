package com.demo.product.service;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.product.dto.ProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.entity.Product;
import com.demo.product.exception.InsufficientStockException;
import com.demo.product.exception.ProductNotFoundException;
/**
 * Command Service for Product Catalog (CQRS Pattern).
 * Handles all write operations (Create, Update, Delete) and publishes events to Kafka.
 */
import com.demo.product.mapper.ProductMapper;
import com.demo.product.outbox.OutboxEvent;
import com.demo.product.outbox.OutboxEventRepository;
import com.demo.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCommandServiceImpl implements ProductCommandService {

	private final ProductRepository productRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;
	private final ProductMapper productMapper;

	private static final String PRODUCT_CACHE = "products";

	@Transactional
	public ProductResponse createProduct(ProductRequest request) {
		Product product = Product.builder().name(request.name()).description(request.description())
				.price(request.price()).stockQty(request.stockQty()).category(request.category()).sku(request.sku())
				.imageUrl(request.imageUrl()).build();
		product = productRepository.save(product);

		publishProductEvent(product, "CREATED");
		return productMapper.toResponse(product);
	}

	@Transactional
	@CacheEvict(value = PRODUCT_CACHE, key = "#id")
	public ProductResponse updateProduct(Long id, ProductRequest request) {
		Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id.toString()));

		product.setName(request.name());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setStockQty(request.stockQty());
		product.setCategory(request.category());
		product.setSku(request.sku());
		product.setImageUrl(request.imageUrl());

		product = productRepository.save(product);
		publishProductEvent(product, "UPDATED");
		return productMapper.toResponse(product);
	}

	@Transactional
	@CacheEvict(value = PRODUCT_CACHE, key = "#id")
	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ProductNotFoundException(id.toString());
		}
		productRepository.deleteById(id);

		// Publish deletion event via outbox
		try {
			OutboxEvent outboxEvent = OutboxEvent.builder().aggregateId(id.toString()).aggregateType("Product")
					.eventType("DELETED").payload("{\"id\":\"" + id + "\", \"status\":\"DELETED\"}").build();
			outboxEventRepository.save(outboxEvent);
		} catch (Exception e) {
			log.error("Failed to serialize OutboxEvent", e);
			throw new RuntimeException("Failed to serialize OutboxEvent", e);
		}
	}

	@Transactional
	public void deductStock(Long id, int quantity) {
		Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id.toString()));

		if (product.getStockQty() < quantity) {
			throw new InsufficientStockException(id.toString(), quantity, product.getStockQty());
		}

		product.setStockQty(product.getStockQty() - quantity);
		productRepository.save(product);
		publishProductEvent(product, "STOCK_DEDUCTED");
	}

	@Transactional
	@CacheEvict(value = PRODUCT_CACHE, key = "#id")
	public void addStock(Long id, int quantity) {
		Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id.toString()));

		product.setStockQty(product.getStockQty() + quantity);
		productRepository.save(product);
		publishProductEvent(product, "STOCK_ADDED");
	}

	private void publishProductEvent(Product product, String eventType) {
		try {
			OutboxEvent outboxEvent = OutboxEvent.builder().aggregateId(product.getId().toString())
					.aggregateType("Product").eventType(eventType)
					.payload(objectMapper.writeValueAsString(productMapper.toResponse(product))).build();
			outboxEventRepository.save(outboxEvent);
		} catch (Exception e) {
			log.error("Failed to serialize OutboxEvent", e);
			throw new RuntimeException("Failed to serialize OutboxEvent", e);
		}
	}
}

