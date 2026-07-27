package com.demo.gateway.ratelimit;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Demonstrates the use of modern Java concurrent collections.
 * 
 * <p>
 * Used as an in-memory fallback rate limiter if Redis is unavailable. Uses:
 * <ul>
 * <li>{@code ConcurrentHashMap} for lock-free, thread-safe IP tracking</li>
 * <li>{@code CopyOnWriteArrayList} for thread-safe iteration of request
 * timestamps</li>
 * </ul>
 */
@Component
@Slf4j
public class LocalInMemoryRateLimiter {

	private static final int MAX_REQUESTS_PER_MINUTE = 60;

	// ConcurrentHashMap for high-concurrency, lock-free reads/writes by IP
	private final ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> requestLog = new ConcurrentHashMap<>();

	// Tracks total rejected requests across all IPs atomically
	private final AtomicInteger totalRejected = new AtomicInteger(0);

	/**
	 * Checks if the given IP is allowed to make a request. Thread-safe and designed
	 * for high concurrent throughput.
	 */
	public boolean isAllowed(String ipAddress) {
		// computeIfAbsent is atomic and thread-safe
		CopyOnWriteArrayList<Instant> timestamps = requestLog.computeIfAbsent(ipAddress,
				k -> new CopyOnWriteArrayList<>());

		Instant oneMinuteAgo = Instant.now().minusSeconds(60);

		// CopyOnWriteArrayList allows safe iteration while other threads might be
		// modifying it.
		// We clean up older requests to prevent memory leaks.
		timestamps.removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));

		if (timestamps.size() < MAX_REQUESTS_PER_MINUTE) {
			timestamps.add(Instant.now());
			return true;
		} else {
			int rejectedCount = totalRejected.incrementAndGet();
			if (rejectedCount % 100 == 0) {
				log.warn("High rate of rejections! Total local rejected: {}", rejectedCount);
			}
			return false;
		}
	}

	public int getRejectedCount() {
		return totalRejected.get();
	}
}
