package com.sgitu.servicegestionincidents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;

@Service
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region}")
    private String region;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private S3Presigner presigner;

    public StorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        // Initialisation du Presigner S3
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of(region))
                .build();
    }

    @Override
    public String generatePreSignedUploadUrl(String objectKey, String contentType, Duration expiration) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    public String generatePreSignedDownloadUrl(String objectKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    public void deleteObject(String objectKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    @PreDestroy
    public void cleanup() {
        if (presigner != null) {
            presigner.close();
        }
    }
}
