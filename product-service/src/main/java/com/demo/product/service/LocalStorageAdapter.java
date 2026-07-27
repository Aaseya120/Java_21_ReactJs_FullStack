package com.demo.product.service;

import com.demo.product.dto.PresignedUrlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Local implementation of ImageStoragePort for development and testing.
 * Enabled when storage.type=local (or missing).
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageAdapter implements ImageStoragePort {

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String extension) {
        String filename = UUID.randomUUID().toString() + extension;
        log.info("Generating local mock upload URL for {}", filename);
        
        // In local dev, we just mock the URL. 
        // The client won't actually upload to S3, but they can simulate the flow.
        // Route through gateway on port 8080 to handle CORS
        String uploadUrl = "http://localhost:8080/api/v1/products/mock-upload/" + filename;
        // Use a placeholder image service for the final URL so image is not broken
        String finalUrl = "https://placehold.co/600x400?text=Mock+" + filename;
        
        return new PresignedUrlResponse(uploadUrl, finalUrl);
    }
}
