package com.demo.product.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.constant.ApiConstants;
import com.demo.common.dto.ApiResponse;
import com.demo.product.dto.ProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.service.ProductCommandService;
import com.demo.product.service.ProductQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Product catalog REST controller using CQRS pattern.
 */
@RestController
@RequestMapping(ApiConstants.ProductApi.BASE)
@RequiredArgsConstructor
public class ProductController {

	private final ProductCommandService commandService;
	private final ProductQueryService queryService;

	// ── Command Operations (Writes) ──────────────────────────

	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(commandService.createProduct(request)));
	}

	@PutMapping(ApiConstants.ProductApi.ID)
	public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long id,
			@Valid @RequestBody ProductRequest request) {
		return ResponseEntity.ok(ApiResponse.success(commandService.updateProduct(id, request)));
	}

	@DeleteMapping(ApiConstants.ProductApi.ID)
	public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
		commandService.deleteProduct(id);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PostMapping(ApiConstants.ProductApi.STOCK_DEDUCT)
	public ResponseEntity<ApiResponse<Void>> deductStock(@PathVariable Long id, @RequestParam int quantity) {
		commandService.deductStock(id, quantity);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PostMapping(ApiConstants.ProductApi.STOCK_ADD)
	public ResponseEntity<ApiResponse<Void>> addStock(@PathVariable Long id, @RequestParam int quantity) {
		commandService.addStock(id, quantity);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	// ── Query Operations (Reads) ─────────────────────────────

	@GetMapping(ApiConstants.ProductApi.ID)
	public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(queryService.getProductById(id)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(queryService.getAllProducts(pageable)));
	}

	@GetMapping(ApiConstants.ProductApi.CATEGORY)
	public ResponseEntity<ApiResponse<List<ProductResponse>>> getByCategory(@PathVariable String category) {
		return ResponseEntity.ok(ApiResponse.success(queryService.getProductsByCategory(category)));
	}
}

