package com.demo.common.constant;

/**
 * Status used for tracking idempotency of requests and events in Redis.
 */
public enum IdempotencyStatus {
	PROCESSING, PROCESSED
}
