package com.corretoria.service;

import com.corretoria.vo.shared.PhotoContent;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementação da porta de armazenamento sobre um serviço S3-compatible.
 *
 * <p>O mesmo código atende MinIO em desenvolvimento e S3 em produção: o que muda é o
 * endpoint configurado em {@code corretoria.storage}.
 */
@Service
public class S3StorageService implements StorageService {

    private static final String KEY_SEPARATOR = "/";
    private static final String EXTENSION_SEPARATOR = ".";

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3StorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(PhotoContent photo, String keyPrefix) {
        String storageKey = buildStorageKey(keyPrefix, photo.extension());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(storageKey)
                .contentType(photo.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(photo.content()));
        return storageKey;
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(storageKey)
                .build());
    }

    @Override
    public String baseUrl() {
        return storageProperties.publicBaseUrl() + KEY_SEPARATOR + storageProperties.bucket();
    }

    /**
     * Chave única sob o prefixo do anúncio.
     *
     * <p>O identificador aleatório evita colisão entre duas fotos enviadas no mesmo instante
     * e impede que a URL de uma foto seja adivinhada a partir da de outra.
     */
    private String buildStorageKey(String keyPrefix, String extension) {
        return keyPrefix + KEY_SEPARATOR + UUID.randomUUID() + EXTENSION_SEPARATOR + extension;
    }
}
