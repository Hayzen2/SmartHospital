package com.example.SmartHospital.service.storage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioStorageService {
    // UUID for generating unique file names, and sanitization to prevent issues with special characters in filenames

    private final AmazonS3 s3Client;

    @Value("${minio.avatar-bucket:avatars}")
    private String avatarBucket;

    @Value("${minio.medical-record-bucket:medicalrecord-attachments}")
    private String medicalRecordBucket;

    @Value("${minio.additional-file-bucket:request-attachments}")
    private String additionalFileBucket;

    @Value("${minio.chat-file-bucket:chat-files}")
    private String chatFileBucket;

    @Value("${minio.presigned-expiry-seconds:3600}")
    private Integer presignedExpirySeconds;

    // ex: url name: "chat-files/userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext"
    public String uploadChatFile(MultipartFile chatFile, String userId) {
        if (chatFile == null || chatFile.isEmpty()) {
            return null;
        }

        String contentType = chatFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String objectName = userId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(chatFile.getOriginalFilename());
        uploadFile(chatFileBucket, objectName, chatFile, contentType);
        return chatFileBucket + "/" + objectName;
    }

    // ex: url name: "avatars/userId/avatar.png"
    public String uploadAvatar(MultipartFile avatarFile, String userId) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image file");
        }

        String extension = resolveFileExtension(avatarFile.getOriginalFilename(), "png");
        String objectName = userId + "/avatar." + extension;
        uploadFile(avatarBucket, objectName, avatarFile, contentType);
        cleanupOldAvatarVariants(userId, objectName);
        return avatarBucket + "/" + objectName;
    }

    // Generates a presigned GET URL from a stored path like "bucket/object..."
    public String toPresignedGetUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }

        String normalized = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        String[] parts = normalized.split("/", 2);
        if (parts.length != 2) {
            return storedPath;
        }

        int expiry = presignedExpirySeconds == null || presignedExpirySeconds <= 0
            ? 3600
            : presignedExpirySeconds;

        try {
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000L * expiry;
            expiration.setTime(expTimeMillis);
            return s3Client.generatePresignedUrl(parts[0], parts[1], expiration).toString();
        } catch (Exception e) {
            System.err.println("Warning: Failed to generate presigned URL for " + storedPath + ": " + e.getMessage());
            return storedPath; // Fallback to raw path so the page doesn't crash
        }
    }

    // ex: url name: "medicalrecord-attachments/userId/550e8400-e29b-41d4-a716-446655440000.pdf"
    public List<String> uploadMedicalRecordPdfs(List<MultipartFile> medicalRecordFiles, String userId) {
        List<String> uploadedPaths = new ArrayList<>();
        if (medicalRecordFiles == null || medicalRecordFiles.isEmpty()) {
            return uploadedPaths;
        }

        for (MultipartFile file : medicalRecordFiles) {
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                String originalFileName = file.getOriginalFilename();
                String originalName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
                boolean isPdf = "application/pdf".equalsIgnoreCase(contentType) || originalName.endsWith(".pdf");
                if (!isPdf) {
                    throw new IllegalArgumentException("Medical record files must be PDF");
                }

                String objectName = userId + "/" + UUID.randomUUID() + ".pdf";
                uploadFile(medicalRecordBucket, objectName, file, "application/pdf");
                uploadedPaths.add(medicalRecordBucket + "/" + objectName);
            }
        }

        return uploadedPaths;
    }

    // ex: url name: "request-attachments/userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext"
    public List<String> uploadAdditionalFiles(List<MultipartFile> additionalFiles, String userId) {
        List<String> uploadedPaths = new ArrayList<>();
        if (additionalFiles == null || additionalFiles.isEmpty()) {
            return uploadedPaths;
        }

        for (MultipartFile file : additionalFiles) {
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/octet-stream";
                }

                // Ex: userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext
                String objectName = userId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFilename());
                uploadFile(additionalFileBucket, objectName, file, contentType);
                uploadedPaths.add(additionalFileBucket + "/" + objectName);
            }
        }

        return uploadedPaths;
    }

    public void deleteFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        for (String filePath : filePaths) {
            if (filePath != null && !filePath.isBlank()) {
                String[] parts = filePath.split("/", 2);
                if (parts.length == 2) {
                    try {
                        s3Client.deleteObject(parts[0], parts[1]);
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to delete file " + filePath + " from storage: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void uploadFile(String bucket, String objectName, MultipartFile file, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);
            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(new PutObjectRequest(bucket, objectName, inputStream, metadata));
            }
        } catch (Exception e) {
            String rootCause = e.getMessage();
            if (e instanceof AmazonS3Exception) {
                rootCause = ((AmazonS3Exception) e).getErrorMessage();
            }
            System.err.println("Error: Failed to upload file to storage: " + rootCause);
            throw new MinioUploadException("Failed to upload file to storage: " + rootCause, e);
        }
    }

    private void cleanupOldAvatarVariants(String userId, String keepObjectName) {
        String avatarPrefix = userId + "/avatar.";
        try {
            ObjectListing listing = s3Client.listObjects(avatarBucket, avatarPrefix);
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                if (!keepObjectName.equals(summary.getKey())) {
                    s3Client.deleteObject(avatarBucket, summary.getKey());
                }
            }
        } catch (Exception e) {
            // Log the warning instead of crashing the entire profile update request!
            System.err.println("Warning: Failed to clean up old avatar files: " + e.getMessage());
        }
    }

    // Resolves file extension from original filename, defaults to provided default if not found
    private String resolveFileExtension(String fileName, String defaultExt) {
        if (fileName == null || !fileName.contains(".")) {
            return defaultExt;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    // Sanitizes filename by replacing non-alphanumeric characters with underscores
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Custom runtime exception for MinIO upload errors
    public static class MinioUploadException extends RuntimeException {
        public MinioUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}