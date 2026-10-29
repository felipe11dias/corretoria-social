package com.corretoria.service;

/**
 * Porta de envio de e-mail.
 *
 * <p>Mesma razão de existir do {@link StorageService}: a regra de negócio da notificação
 * depende de "mandar uma mensagem para um endereço", não do {@code JavaMailSender}. Trocar
 * SMTP por um provedor de API — ou por um duplo em teste — é trocar a implementação.
 *
 * <p>O nome é {@code mail} (envio), distinto do VO {@code Email} (endereço).
 */
public interface MailService {

    /**
     * Envia uma mensagem de texto simples.
     *
     * @param recipient endereço de destino
     * @param subject   assunto
     * @param body      corpo em texto puro
     */
    void send(String recipient, String subject, String body);
}
