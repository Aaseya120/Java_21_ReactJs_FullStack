package com.demo.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;

/**
 * Spring Data JPA repository for {@link Order} entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	/** All orders for a user, sorted newest-first. */
	List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

	/** All orders in a given status. */
	List<Order> findByStatus(OrderStatus status);

	/** Orders for a specific product. */
	List<Order> findByProductId(Long productId);

	/** Count active (non-terminal) orders for a user. */
	long countByUserIdAndStatusNotIn(Long userId, List<OrderStatus> terminalStatuses);

	/** Count total orders for a user (used to generate sequential display ID). */
	long countByUserId(Long userId);
}

