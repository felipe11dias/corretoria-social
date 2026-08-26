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
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Comentário em uma publicação do mural.
 *
 * <p>Dependente do {@link RealtorPost}, como {@code ProposalMessage} é da proposta. O autor
 * pode ser qualquer usuário autenticado — cliente ou corretor: conversa no mural é pública, ao
 * contrário do thread de uma proposta.
 */
@Getter
@Setter
@Entity
@Table(name = "realtor_post_comments")
public class RealtorPostComment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_post_comments_realtor_posts"))
    private RealtorPost post;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_post_comments_users"))
    private User author;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public RealtorPostComment() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RealtorPostComment comment)) {
            return false;
        }
        return id != null && id.equals(comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
