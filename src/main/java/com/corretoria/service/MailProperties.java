package com.corretoria.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de envio de e-mail, sob o prefixo {@code corretoria.mail}.
 *
 * <p>Só o remetente mora aqui: host, porta e credenciais do SMTP são configuração do
 * {@code JavaMailSender} e ficam sob {@code spring.mail}, onde o Spring Boot já as espera.
 *
 * <p>Mora em {@code service} pelo mesmo motivo de {@link StorageProperties}: quem a consome é
 * o {@link SmtpMailService}, e nada no projeto pode depender de {@code config}.
 *
 * @param from endereço remetente das notificações
 */
@Validated
@ConfigurationProperties(prefix = "corretoria.mail")
public record MailProperties(

        @NotBlank(message = "O remetente dos e-mails é obrigatório")
        String from) {
}
