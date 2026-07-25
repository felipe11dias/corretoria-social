package com.corretoria.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação, exposto como bean.
 *
 * <p>Nenhuma classe chama {@code Instant.now()} ou {@code LocalDateTime.now()} diretamente
 * quando o instante importa para a regra — injeta este {@link Clock}. É o que torna
 * testáveis a expiração do token de redefinição de senha e o {@code timestamp} das respostas
 * de erro: no teste, entra um {@code Clock.fixed(...)}.
 *
 * <p>UTC de propósito: o instante gravado não muda de significado conforme o fuso do
 * servidor.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
