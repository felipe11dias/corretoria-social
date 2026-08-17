package com.corretoria.config;

import com.corretoria.service.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registro das propriedades de e-mail do projeto.
 *
 * <p>Não declara bean de envio: o {@code JavaMailSender} vem da autoconfiguração do Spring
 * Boot a partir de {@code spring.mail}. O que falta a ela é o remetente, que é decisão do
 * domínio e vive em {@link MailProperties}.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
}
