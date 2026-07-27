package com.demo.product.service;

import com.demo.product.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 implementation of ImageStoragePort for production.
 * Enabled when storage.type=s3.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class AwsS3StorageAdapter implements ImageStoragePort {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String extension) {
        String filename = UUID.randomUUID().toString() + extension;
        log.info("Generating S3 Presigned URL for bucket {} and key {}", bucketName, filename);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();
        
        // Construct the final public URL where the image will be readable
        String finalUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, filename);

        return new PresignedUrlResponse(uploadUrl, finalUrl);
    }
}
