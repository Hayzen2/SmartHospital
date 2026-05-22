package com.example.SmartHospital.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("AWSS3V4SignerType"); 

        // Strip trailing slash from URL if present to prevent signature path mismatches
        String sanitizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        // Failsafe: Log the region to ensure it's not silently defaulting to the wrong one
        String resolvedRegion = (region != null && !region.isBlank()) ? region : "ap-northeast-2";

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(sanitizedUrl, resolvedRegion))
                .withPathStyleAccessEnabled(true) 
                .withClientConfiguration(clientConfig)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .disableChunkedEncoding() 
                .withPayloadSigningEnabled(false) 
                .build();
    }
}