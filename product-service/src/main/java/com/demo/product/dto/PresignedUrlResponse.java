package com.demo.product.dto;

/**
 * DTO for returning a presigned URL to the client.
 * The client uses the uploadUrl to PUT the file directly to S3.
 * The finalUrl is what the client should include in the Product creation JSON.
 */
public record PresignedUrlResponse(
    String uploadUrl,
    String finalUrl
) {}
