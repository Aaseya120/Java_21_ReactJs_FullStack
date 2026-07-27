package com.demo.product.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.product.dto.ProductResponse;
import com.demo.product.exception.ProductNotFoundException;
import com.demo.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Query Service for Product Catalog (CQRS Pattern). Handles all read
 * operations, optimized with Redis caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductQueryService {

	private static final String PRODUCT_CACHE = "products";
	private final ProductRepository productRepository;
	private final com.demo.product.mapper.ProductMapper productMapper;

	/**
	 * Cache-aside pattern: Reads from Redis first. If miss, reads from DB and
	 * populates Redis.
	 */
	@Cacheable(value = PRODUCT_CACHE, key = "#id")
	public ProductResponse getProductById(Long id) {
		log.debug("Cache miss for product {}. Fetching from DB.", id);
		return productRepository.findById(id).map(productMapper::toResponse)
				.orElseThrow(() -> new ProductNotFoundException(id.toString()));
	}

	public Page<ProductResponse> getAllProducts(Pageable pageable) {
		return productRepository.findAll(pageable).map(productMapper::toResponse);
	}

	public List<ProductResponse> getProductsByCategory(String category) {
		return productRepository.findByCategoryAndActiveTrueOrderByCreatedAtDesc(category).stream().map(productMapper::toResponse).toList();
	}
}

