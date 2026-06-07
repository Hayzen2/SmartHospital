package com.example.SmartHospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.region}")
    private String region;

    @Bean
    public AmazonS3 s3Client() {
        // Trim credentials to prevent hidden whitespace issues
        String cleanAccessKey = accessKey != null ? accessKey.trim() : "";
        String cleanSecretKey = secretKey != null ? secretKey.trim() : "";

        BasicAWSCredentials credentials = new BasicAWSCredentials(cleanAccessKey, cleanSecretKey);
        
        ClientConfiguration clientConfig = new ClientConfiguration();

        // Strip trailing slash
        String sanitizedUrl = (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
        String cleanUrl = sanitizedUrl != null ? sanitizedUrl.trim() : "";

        // Fallback region: ONLY use ap-northeast-2 if that is truly where your Supabase project lives.
        // Otherwise, replace "ap-northeast-2" with your actual Supabase region
        String resolvedRegion = (region != null && !region.isBlank()) ? region.trim() : "ap-northeast-2";

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(cleanUrl, resolvedRegion))
                .withPathStyleAccessEnabled(true) 
                .withClientConfiguration(clientConfig)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .disableChunkedEncoding() 
                .build();
    }
}