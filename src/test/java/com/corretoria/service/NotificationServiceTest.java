package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.corretoria.entity.Notification;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.mapper.NotificationMapper;
import com.corretoria.repository.NotificationRepository;
import com.corretoria.security.UserSecurity;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long RECIPIENT_ID = 3L;
    private static final Long NOTIFICATION_ID = 42L;
    private static final String RECIPIENT_EMAIL = "cliente@email.com";
    private static final String MESSAGE = "Um anúncio novo casou com a sua busca";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private MailService mailService;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private static User aRecipient() {
        User user = new User();
        user.setId(RECIPIENT_ID);
        user.setEmail(RECIPIENT_EMAIL);
        return user;
    }

    @Nested
    @DisplayName("notify")
    class Notify {

        @Test
        @DisplayName("notify - grava o aviso in-app e envia o e-mail")
        void shouldPersistAndSendEmail() {
            User recipient = aRecipient();

            notificationService.notify(recipient, NotificationType.SEARCH_MATCH, MESSAGE);

            then(notificationRepository).should().save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getRecipient()).isSameAs(recipient);
            assertThat(saved.getType()).isEqualTo(NotificationType.SEARCH_MATCH);
            assertThat(saved.getMessage()).isEqualTo(MESSAGE);
            assertThat(saved.isRead()).isFalse();

            then(mailService).should().send(eq(RECIPIENT_EMAIL), contains("Corretoria"),
                    eq(MESSAGE));
        }

        @Test
        @DisplayName("notify - SMTP fora do ar - mantém a notificação in-app e não propaga")
        void shouldKeepNotificationWhenMailFails() {
            // A notificação in-app é a fonte de verdade: perder o e-mail não pode fazer o
            // usuário perder o aviso.
            willThrow(new MailSendException("SMTP indisponível"))
                    .given(mailService).send(any(), any(), any());

            assertThatCode(() -> notificationService.notify(
                    aRecipient(), NotificationType.SEARCH_MATCH, MESSAGE))
                    .doesNotThrowAnyException();

            then(notificationRepository).should().save(any());
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("markAsRead - notificação existente - marca como lida")
        void shouldMarkAsRead() {
            Notification notification = new Notification();
            notification.setId(NOTIFICATION_ID);
            notification.setRecipient(aRecipient());
            notification.setType(NotificationType.SEARCH_MATCH);
            notification.setMessage(MESSAGE);

            given(notificationRepository.findById(NOTIFICATION_ID))
                    .willReturn(Optional.of(notification));
            given(notificationRepository.save(notification)).willReturn(notification);

            notificationService.markAsRead(NOTIFICATION_ID);

            assertThat(notification.isRead()).isTrue();
        }
    }
}
