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

/**
 * Industry standard REST audit table (LOG_REST) capturing full HTTP transaction details.
 * 
 * <p>Captures login/api userId, timestamp, requestUrl, requestBody (sanitized),
 * responseBody, statusCode, errorCode, errorDesc, and requestId along with
 * industry-standard audit context (HTTP method, client IP, user agent, duration, service name).
 */
@Entity
@Table(name = "log_rest", indexes = {
		@Index(name = "idx_log_rest_user_id", columnList = "userId"),
		@Index(name = "idx_log_rest_request_id", columnList = "requestId"),
		@Index(name = "idx_log_rest_timestamp", columnList = "timestamp"),
		@Index(name = "idx_log_rest_status", columnList = "statusCode")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogRest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String userId;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime timestamp;

	@Column(nullable = false, length = 10)
	private String httpMethod;

	@Column(nullable = false, length = 1000)
	private String requestUrl;

	@Column(columnDefinition = "TEXT")
	private String requestBody;

	@Column(columnDefinition = "TEXT")
	private String responseBody;

	@Column(nullable = false)
	private Integer statusCode;

	@Column(length = 100)
	private String errorCode;

	@Column(columnDefinition = "TEXT")
	private String errorDesc;

	@Column(nullable = false, length = 100)
	private String requestId;

	// ── Industry Standard Extra Audit Context ────────────────
	@Column(nullable = false, length = 100)
	private String serviceName;

	@Column(length = 100)
	private String clientIp;

	@Column(length = 500)
	private String userAgent;

	@Column(nullable = false)
	private Long durationMs;
}
