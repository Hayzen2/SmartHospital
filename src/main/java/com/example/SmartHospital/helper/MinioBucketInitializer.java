package com.example.SmartHospital.helper;

import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final AmazonS3 s3Client;

    @PostConstruct // This method will be called after the bean is initialized
    public void createBucket() {
        try{
            createBucketIfNotExists("avatars");
            createBucketIfNotExists("request-attachments");
            createBucketIfNotExists("medicalrecord-attachments");
            createBucketIfNotExists("chat-files");
        } catch (Exception e) {
            // Log the error and rethrow as a runtime exception to prevent application from starting
            System.err.println("Error initializing S3 buckets: " + e.getMessage());
            throw new RuntimeException("Failed to initialize S3 buckets", e);
        }
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            s3Client.headBucket(new HeadBucketRequest(bucketName)); // Standard check without ACLs
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 403) {
                try {
                    s3Client.createBucket(bucketName);
                } catch (Exception ex) {
                    System.err.println("Note: Bucket creation skipped or failed: " + ex.getMessage());
                }
            } else {
                throw e;
            }
        }
    }
}
