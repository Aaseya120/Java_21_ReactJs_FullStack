package com.demo.payment.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, Long> {

	List<PaymentOutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}
