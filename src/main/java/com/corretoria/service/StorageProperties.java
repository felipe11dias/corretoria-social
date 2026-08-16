package com.corretoria.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades do armazenamento de objetos, sob o prefixo {@code corretoria.storage}.
 *
 * <p>Mora em {@code service}, e não em {@code config}, pelo mesmo motivo de
 * {@code JwtProperties} morar em {@code security}: quem a consome é o
 * {@link S3StorageService}, e nada no projeto pode depender de {@code config} sob pena de
 * ciclo.
 *
 * @param endpoint      endereço do serviço S3-compatible usado pela aplicação
 * @param publicBaseUrl prefixo que o cliente da API usa para baixar o objeto; distinto do
 *                      {@code endpoint} porque dentro do compose o host é o nome do serviço,
 *                      e no navegador é {@code localhost}
 * @param region        região informada ao SDK; o MinIO aceita qualquer valor, a AWS não
 * @param bucket        bucket onde as fotos de anúncio são gravadas
 * @param accessKey     credencial de acesso
 * @param secretKey     credencial secreta
 */
@Validated
@ConfigurationProperties(prefix = "corretoria.storage")
public record StorageProperties(

        @NotBlank(message = "O endpoint do storage é obrigatório")
        String endpoint,

        @NotBlank(message = "A URL pública do storage é obrigatória")
        String publicBaseUrl,

        @NotBlank(message = "A região do storage é obrigatória")
        String region,

        @NotBlank(message = "O bucket do storage é obrigatório")
        String bucket,

        @NotBlank(message = "A chave de acesso do storage é obrigatória")
        String accessKey,

        @NotBlank(message = "A chave secreta do storage é obrigatória")
        String secretKey) {
}
