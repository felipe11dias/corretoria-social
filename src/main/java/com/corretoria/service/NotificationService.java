package com.corretoria.service;

import com.corretoria.entity.Notification;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.NotificationMapper;
import com.corretoria.repository.NotificationRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.response.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega de notificações a um usuário.
 *
 * <p>Dois canais, com pesos diferentes: o registro in-app é a <strong>fonte de verdade</strong>
 * e o e-mail é entrega adicional. Um SMTP fora do ar não pode fazer o usuário perder o aviso —
 * por isso a falha de envio vira registro no log, e não exceção que desfaz a notificação.
 *
 * <p>Quem chama informa a mensagem já redigida: o texto pertence ao fato que originou o aviso
 * (anúncio compatível, proposta recebida), e centralizá-lo aqui obrigaria este serviço a
 * conhecer todos os domínios que notificam.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String SUBJECT_PREFIX = "Corretoria — ";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final MailService mailService;
    private final UserSecurity userSecurity;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            MailService mailService,
            UserSecurity userSecurity) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.mailService = mailService;
        this.userSecurity = userSecurity;
    }

    /** Registra o aviso e tenta entregá-lo também por e-mail. */
    @Transactional
    public void notify(User recipient, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);

        notificationRepository.save(notification);
        sendEmail(recipient, type, message);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMine(Authentication authentication, Pageable pageable) {
        User recipient = userSecurity.getAuthenticatedUser(authentication);

        return notificationRepository.findByRecipientId(recipient.getId(), pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long countUnread(Authentication authentication) {
        User recipient = userSecurity.getAuthenticatedUser(authentication);
        return notificationRepository.countByRecipientIdAndReadFalse(recipient.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.notification(id));

        notification.setRead(true);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    /**
     * Entrega por e-mail, de melhor esforço.
     *
     * <p>A exceção é capturada aqui, e não deixada subir: a notificação in-app já está gravada,
     * e derrubar a operação por causa do canal secundário perderia o aviso principal. O
     * {@code warn} com o tipo e o destinatário é o que permite descobrir um SMTP fora do ar sem
     * depender de o usuário reclamar.
     */
    private void sendEmail(User recipient, NotificationType type, String message) {
        try {
            mailService.send(recipient.getEmail(), SUBJECT_PREFIX + subjectOf(type), message);
        } catch (MailException exception) {
            log.warn("Falha ao enviar e-mail de notificação {} para o usuário {}",
                    type, recipient.getId(), exception);
        }
    }

    private String subjectOf(NotificationType type) {
        return switch (type) {
            case SEARCH_MATCH -> "novo anúncio compatível com a sua busca";
            case NEW_PROPOSAL -> "você recebeu uma proposta";
            case PROPOSAL_STATUS_CHANGED -> "sua proposta mudou de situação";
            case NEW_PROPOSAL_MESSAGE -> "nova mensagem em uma proposta";
            case NEW_REVIEW -> "você recebeu uma avaliação";
            case NEW_FOLLOWER -> "você tem um novo seguidor";
            case LISTING_REPORTED -> "um anúncio foi denunciado";
        };
    }
}
