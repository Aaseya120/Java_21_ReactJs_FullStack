package com.demo.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.payment.entity.PaymentAuditLog;

@Repository
public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

	List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
}
