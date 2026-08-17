package com.corretoria.entity;

import com.corretoria.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Notificação in-app de um usuário.
 *
 * <p>Agregado raiz. É a <strong>fonte de verdade</strong> do aviso: o e-mail é entrega
 * adicional, de melhor esforço, e um SMTP fora do ar não pode fazer o usuário perder o
 * registro.
 *
 * <p>Guarda a mensagem já redigida, e não os dados para montá-la depois: assim o histórico
 * continua legível mesmo que o anúncio ou a proposta que a originou mude ou saia do ar.
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "recipient_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_users"))
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "read", nullable = false)
    private boolean read;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Notification() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Notification notification)) {
            return false;
        }
        return id != null && id.equals(notification.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
