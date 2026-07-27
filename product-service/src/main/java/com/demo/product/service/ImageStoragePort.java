package com.demo.product.service;

import com.demo.product.dto.PresignedUrlResponse;

/**
 * Port (Interface) for image storage operations.
 * Implemented by adapters for different storage mechanisms (e.g. Local vs S3).
 */
public interface ImageStoragePort {
    
    /**
     * Generates a URL for the client to directly upload an image.
     * @param extension The file extension (e.g., ".jpg", ".png")
     * @return PresignedUrlResponse containing the upload URL and the final read URL.
     */
    PresignedUrlResponse generatePresignedUploadUrl(String extension);
}
