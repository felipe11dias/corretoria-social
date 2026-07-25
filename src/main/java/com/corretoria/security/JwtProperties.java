package com.corretoria.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de assinatura e validade do JWT, sob o prefixo {@code corretoria.jwt}.
 *
 * <p>O tamanho mínimo do segredo é validado na <strong>inicialização</strong>: um segredo
 * curto derrubaria a aplicação no primeiro login, com a exceção do jjwt, em vez de na subida.
 * Falhar cedo é melhor que falhar no primeiro usuário.
 *
 * <p>Mora em {@code security}, e não em {@code config}, porque quem a consome é o
 * {@link JwtService}. Em {@code config} ela criaria o ciclo
 * {@code config -> security -> config}, já que o {@code SecurityConfig} depende do filtro e
 * do entry point — e nada deve depender de {@code config}.
 *
 * @param secret     chave HMAC-SHA256; no mínimo 32 caracteres (256 bits)
 * @param expiration validade do token em milissegundos
 */
@Validated
@ConfigurationProperties(prefix = "corretoria.jwt")
public record JwtProperties(

        @NotBlank(message = "O segredo do JWT é obrigatório")
        @Size(min = 32, message = "O segredo do JWT deve ter no mínimo 32 caracteres (256 bits)")
        String secret,

        @Positive(message = "A expiração do JWT deve ser positiva")
        long expiration) {
}
