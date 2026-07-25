package com.corretoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Token de uso único para redefinição de senha.
 *
 * <p>Persiste o <strong>hash</strong> do token, nunca o valor enviado ao usuário: quem lê o
 * banco não consegue redefinir a senha de ninguém.
 *
 * <p>A decisão de expiração compara {@code expiresAt} com um relógio injetado no serviço — a
 * entidade não consulta {@code LocalDateTime.now()} por conta própria, para que a regra seja
 * testável.
 */
@Getter
@Setter
@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_password_reset_tokens_token_hash",
                columnNames = "token_hash"))
public class PasswordResetToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_tokens_users"))
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    protected PasswordResetToken() {
        // exigido pelo JPA
    }

    public boolean isExpiredAt(LocalDateTime moment) {
        return expiresAt.isBefore(moment);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PasswordResetToken token)) {
            return false;
        }
        return id != null && id.equals(token.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
