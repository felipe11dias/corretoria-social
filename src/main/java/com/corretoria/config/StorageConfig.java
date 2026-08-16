package com.corretoria.config;

import com.corretoria.service.StorageProperties;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Cliente do armazenamento de objetos.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties storageProperties) {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.endpoint()))
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                storageProperties.accessKey(), storageProperties.secretKey())))
                // O MinIO só responde no estilo de caminho (host/bucket/objeto). O padrão do
                // SDK é o estilo de subdomínio (bucket.host/objeto), que exigiria DNS curinga.
                .forcePathStyle(true)
                .build();
    }
}
