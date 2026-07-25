package com.corretoria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Emissão e validação dos tokens JWT, assinados em HMAC-SHA256.
 *
 * <p>O algoritmo é <strong>fixado em HS256</strong>. Deixar o jjwt derivá-lo do tamanho da
 * chave ({@code Keys.hmacShaKeyFor}) faria o algoritmo mudar sozinho conforme o ambiente —
 * um segredo de 32 caracteres produz HS256, um de 48 produz HS384 — e a assinatura efetiva
 * deixaria de ser uma decisão de projeto para virar consequência acidental da configuração.
 *
 * <p>O token carrega apenas o login no {@code subject} e as datas de emissão e expiração.
 * Nada de papéis nem de dados pessoais: o conteúdo de um JWT é apenas assinado, não
 * cifrado — qualquer um que o intercepte lê o que estiver dentro. Os papéis são recarregados
 * do banco a cada requisição, o que também faz uma revogação de papel valer imediatamente,
 * em vez de esperar o token expirar.
 */
@Component
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKey signingKey;
    private final long expiration;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    public String generateToken(String login) {
        Instant issuedAt = Instant.now(clock);

        return Jwts.builder()
                .subject(login)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expiration)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Devolve o login do token, ou vazio se ele for inválido.
     *
     * <p>Assinatura adulterada, token expirado e texto malformado são todos tratados como
     * "não autenticado": distinguir os casos na resposta entregaria a um atacante o retorno
     * de cada tentativa de forjar token.
     */
    public Optional<String> extractLogin(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long expiration() {
        return expiration;
    }
}
