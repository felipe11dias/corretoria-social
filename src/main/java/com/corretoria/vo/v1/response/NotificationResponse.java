package com.corretoria.vo.v1.response;

import com.corretoria.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * Notificação in-app.
 *
 * <p>Carrega a mensagem já redigida no momento do fato: é o que mantém o histórico legível
 * mesmo depois de o anúncio que a originou mudar de preço ou sair do ar.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt) {
}
