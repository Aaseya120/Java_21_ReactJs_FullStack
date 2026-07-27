package com.demo.common.audit;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs", indexes = { @Index(name = "idx_audit_logs_user_id", columnList = "userId"),
		@Index(name = "idx_audit_logs_action", columnList = "action") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String serviceName;

	@Column(nullable = false, length = 100)
	private String action;

	@Column(length = 100)
	private String entityType;

	@Column(length = 255)
	private String entityId;

	@Column(length = 255)
	private String userId;

	@Column(length = 50)
	private String userIp;

	@Column(length = 10)
	private String httpMethod;

	private String requestUri;

	@Column(columnDefinition = "TEXT")
	private String requestBody;

	private Integer responseStatus;
	private Long durationMs;

	@Column(columnDefinition = "TEXT")
	private String details;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;
}

