package com.demo.order.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
	List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}

