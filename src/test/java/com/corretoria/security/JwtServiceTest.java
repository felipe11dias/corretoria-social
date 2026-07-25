package com.corretoria.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-256-bits-de-tamanho";
    private static final String OTHER_SECRET = "outro-segredo-de-teste-com-mais-de-256-bits!!";
    private static final long EXPIRATION_MILLIS = 3_600_000L;
    private static final Instant NOW = Instant.parse("2026-07-25T10:00:00Z");
    private static final String LOGIN = "corretor.demo";

    private final JwtService jwtService = jwtServiceAt(NOW, SECRET);

    private static JwtService jwtServiceAt(Instant instant, String secret) {
        return new JwtService(
                new JwtProperties(secret, EXPIRATION_MILLIS),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("emissao")
    class Emissao {

        @Test
        @DisplayName("generateToken - login válido - token pode ser lido de volta")
        void shouldRoundTripLogin() {
            String token = jwtService.generateToken(LOGIN);

            assertThat(jwtService.extractLogin(token)).contains(LOGIN);
        }

        @Test
        @DisplayName("generateToken - dois logins - tokens diferentes")
        void shouldIssueDistinctTokens() {
            assertThat(jwtService.generateToken(LOGIN))
                    .isNotEqualTo(jwtService.generateToken("cliente.demo"));
        }

        @Test
        @DisplayName("expiration - devolve a validade configurada")
        void shouldExposeConfiguredExpiration() {
            assertThat(jwtService.expiration()).isEqualTo(EXPIRATION_MILLIS);
        }

        @Test
        @DisplayName("generateToken - segredo longo - assina em HS256, não no algoritmo derivado do tamanho")
        void shouldAlwaysSignWithHs256() {
            // O segredo deste teste tem mais de 48 bytes. Se o algoritmo fosse derivado do
            // tamanho da chave, sairia HS384 aqui e HS256 num ambiente com segredo menor.
            String header = jwtService.generateToken(LOGIN).split("\\.")[0];
            String decodedHeader =
                    new String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8);

            assertThat(decodedHeader).contains("\"alg\":\"HS256\"");
        }
    }

    @Nested
    @DisplayName("validacao")
    class Validacao {

        @Test
        @DisplayName("extractLogin - token expirado - vazio")
        void shouldRejectExpiredToken() {
            String token = jwtService.generateToken(LOGIN);

            // Um milissegundo depois de expirar.
            JwtService afterExpiration =
                    jwtServiceAt(NOW.plusMillis(EXPIRATION_MILLIS + 1), SECRET);

            assertThat(afterExpiration.extractLogin(token)).isEmpty();
        }

        @Test
        @DisplayName("extractLogin - token ainda válido - devolve o login")
        void shouldAcceptTokenBeforeExpiration() {
            String token = jwtService.generateToken(LOGIN);

            JwtService beforeExpiration =
                    jwtServiceAt(NOW.plusMillis(EXPIRATION_MILLIS - 1), SECRET);

            assertThat(beforeExpiration.extractLogin(token)).contains(LOGIN);
        }

        @Test
        @DisplayName("extractLogin - token assinado com outra chave - vazio")
        void shouldRejectTokenSignedWithAnotherKey() {
            // Impede que alguém emita o próprio token e seja aceito como usuário legítimo.
            String foreignToken = jwtServiceAt(NOW, OTHER_SECRET).generateToken("admin.demo");

            assertThat(jwtService.extractLogin(foreignToken)).isEmpty();
        }

        @Test
        @DisplayName("extractLogin - assinatura adulterada - vazio")
        void shouldRejectTamperedToken() {
            String token = jwtService.generateToken(LOGIN);
            String tampered = token.substring(0, token.length() - 2) + "xy";

            assertThat(jwtService.extractLogin(tampered)).isEmpty();
        }

        @Test
        @DisplayName("extractLogin - texto malformado ou vazio - vazio, sem lançar")
        void shouldRejectMalformedToken() {
            assertThat(jwtService.extractLogin("nao-e-um-jwt")).isEmpty();
            assertThat(jwtService.extractLogin("")).isEmpty();
        }
    }
}
