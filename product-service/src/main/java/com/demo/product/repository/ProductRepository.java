package com.demo.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.demo.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByActiveTrue(Pageable pageable);

	List<Product> findByCategoryAndActiveTrueOrderByCreatedAtDesc(String category);

	Optional<Product> findBySku(String sku);

	/** Decrement stock qty atomically — avoids lost update anomalies. */
	@Modifying
	@Query("UPDATE Product p SET p.stockQty = p.stockQty - :qty WHERE p.id = :id AND p.stockQty >= :qty")
	int decrementStock(Long id, int qty);

	/** Increment stock qty (for restocking or cancellations). */
	@Modifying
	@Query("UPDATE Product p SET p.stockQty = p.stockQty + :qty WHERE p.id = :id")
	int incrementStock(Long id, int qty);
}

