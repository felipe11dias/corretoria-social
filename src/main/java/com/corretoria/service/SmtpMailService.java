package com.corretoria.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementação da porta de e-mail sobre SMTP.
 *
 * <p>O mesmo código atende o Mailpit em desenvolvimento e o SMTP real em produção: o que muda
 * é o servidor configurado em {@code spring.mail}.
 *
 * <p>Falha de entrega <strong>sobe</strong>: quem chama decide se um e-mail perdido derruba a
 * operação ou apenas vira registro no log. Engolir aqui esconderia um SMTP fora do ar de todo
 * o sistema.
 */
@Service
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public SmtpMailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
