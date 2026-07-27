package com.demo.common.util;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility guard to ensure at-least-once Kafka consumers process events exactly once.
 * Backed by Redis StringRedisTemplate with a 24-hour TTL on processed event IDs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaIdempotencyGuard {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "idempotency:event:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Checks if the event ID has already been processed. If not, records it in Redis
     * atomically with a 24-hour TTL and returns false.
     *
     * @param eventId unique event identifier (e.g. orderId or UUID)
     * @return true if the event was already processed (duplicate), false if new
     */
    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return false;
        }

        String key = KEY_PREFIX + eventId.trim();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSED", DEFAULT_TTL);
            if (Boolean.FALSE.equals(acquired)) {
                log.warn("Duplicate Kafka event detected and blocked by idempotency guard: {}", eventId);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis error checking idempotency for event {}: {}. Proceeding with processing.", eventId, e.getMessage());
            return false;
        }
        return false;
    }
}
