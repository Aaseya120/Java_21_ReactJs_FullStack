package com.demo.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByIdempotencyKey(String idempotencyKey);

	List<Payment> findByOrderId(Long orderId);

	List<Payment> findByUserId(Long userId);

	List<Payment> findByStatus(PaymentStatus status);
}
