package com.demo.product.service;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.product.entity.Product;
import com.demo.product.exception.ProductNotFoundException;
import com.demo.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service demonstrating the use of Redisson for distributed locking and object
 * holding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedInventoryService {

	private final RedissonClient redissonClient;
	private final ProductRepository productRepository;

	private static final String LOCK_PREFIX = "lock:inventory:";
	private static final String BUCKET_PREFIX = "inventory:bucket:";

	/**
	 * Decrements inventory safely across multiple service instances using Redisson
	 * RLock. Uses RBucket to store the fast-access inventory count in Redis.
	 */
	@Transactional
	public boolean decrementInventory(Long productId, int quantityToDeduct) {
		String lockKey = LOCK_PREFIX + productId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// Try to acquire lock for up to 5 seconds, hold for 10 seconds max
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				log.info("Acquired distributed lock for product {}", productId);

				// Use RBucket to read/write thread-safe distributed cache
				RBucket<Integer> bucket = redissonClient.getBucket(BUCKET_PREFIX + productId);

				// Sync with DB if bucket is empty
				if (!bucket.isExists()) {
					Product product = productRepository.findById(productId)
							.orElseThrow(() -> new ProductNotFoundException(productId.toString()));
					bucket.set(product.getStockQty());
				}

				int currentStock = bucket.get();

				if (currentStock >= quantityToDeduct) {
					// Update cache
					bucket.set(currentStock - quantityToDeduct);

					// Update database
					Product product = productRepository.findById(productId)
							.orElseThrow(() -> new ProductNotFoundException(productId.toString()));
					product.setStockQty(product.getStockQty() - quantityToDeduct);
					productRepository.save(product);

					log.info("Successfully deducted {} items from product {}", quantityToDeduct, productId);
					return true;
				} else {
					log.warn("Insufficient stock for product {}. Requested {}, Available {}", productId,
							quantityToDeduct, currentStock);
					return false;
				}
			} else {
				log.warn("Could not acquire lock for product {}, operation timed out", productId);
				return false;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Thread interrupted while waiting for lock", e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.info("Released distributed lock for product {}", productId);
			}
		}
	}
}

