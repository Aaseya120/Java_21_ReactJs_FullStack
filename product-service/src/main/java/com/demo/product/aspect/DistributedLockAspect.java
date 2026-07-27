package com.demo.product.aspect;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.demo.common.annotation.DistributedLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect to handle Redisson distributed locks seamlessly.
 */
@Aspect
@Component
@Order(1) // Run before @Transactional
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = generateLockKey(joinPoint, distributedLock);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            log.debug("Attempting to acquire lock: {}", lockKey);
            if (lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS)) {
                log.debug("Acquired lock: {}", lockKey);
                try {
                    return joinPoint.proceed();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released lock: {}", lockKey);
                    }
                }
            } else {
                log.error("Could not acquire lock: {}", lockKey);
                throw new RuntimeException("Could not acquire lock for key: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new RuntimeException("Thread interrupted while waiting for lock", e);
        }
    }

    private String generateLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        String keyParamName = distributedLock.keyParam();
        Object keyValue = null;

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(keyParamName)) {
                keyValue = args[i];
                break;
            }
        }

        if (keyValue == null) {
            throw new IllegalArgumentException("Key parameter '" + keyParamName + "' not found in method arguments");
        }

        return distributedLock.keyPrefix() + keyValue;
    }
}
