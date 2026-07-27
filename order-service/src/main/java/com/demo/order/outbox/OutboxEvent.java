package com.demo.order.outbox;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transactional Outbox Pattern Entity. Stores events in the same database
 * transaction as business entities.
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String aggregateId;

	@Column(nullable = false)
	private String aggregateType;

	@Column(nullable = false)
	private String eventType;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String payload;

	@Column(nullable = false)
	@Builder.Default
	private boolean processed = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;
}

