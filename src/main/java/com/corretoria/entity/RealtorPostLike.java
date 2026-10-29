package com.corretoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Curtida em uma publicação do mural.
 *
 * <p>Junção pura, como {@link ListingFavorite} e {@link RealtorFollow}. O {@code UNIQUE}
 * garante o que a interface promete: uma curtida por pessoa, e não um contador que sobe a cada
 * clique.
 *
 * <p>O autor é {@code user}, e não {@code customer}: curtir e comentar são liberados a
 * qualquer usuário autenticado — corretor também participa da conversa do mural —, ao
 * contrário de favoritar e seguir, que são do cliente.
 */
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "realtor_post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_realtor_post_likes_post_id_user_id",
                columnNames = {"post_id", "user_id"}))
public class RealtorPostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_post_likes_realtor_posts"))
    private RealtorPost post;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_post_likes_users"))
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public RealtorPostLike() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RealtorPostLike like)) {
            return false;
        }
        return id != null && id.equals(like.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
