package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.NotificationRepository;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por posse da notificação.
 *
 * <p>Notificação é correspondência: só o destinatário a marca como lida. Sem esta checagem, o
 * id sequencial da URL permitiria varrer os avisos de todo mundo.
 */
@Component
public class NotificationSecurity {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationSecurity(
            NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public boolean isRecipient(Long notificationId, Authentication authentication) {
        if (notificationId == null
                || authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }

        return notificationRepository.findRecipientIdById(notificationId)
                .flatMap(recipientId -> userRepository.findByLogin(authentication.getName())
                        .map(User::getId)
                        .filter(recipientId::equals))
                .isPresent();
    }
}
