package com.demo.product.service;


import com.demo.product.dto.ProductRequest;
import com.demo.product.dto.ProductResponse;

public interface ProductCommandService {
	ProductResponse createProduct(ProductRequest request);

	ProductResponse updateProduct(Long id, ProductRequest request);

	void deleteProduct(Long id);

	void deductStock(Long id, int quantity);

	void deductStockForSaga(Long id, int quantity, Long orderId);

	void addStock(Long id, int quantity);
}

