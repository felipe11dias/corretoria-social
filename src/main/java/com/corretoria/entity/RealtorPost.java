package com.corretoria.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Publicação no mural de um corretor.
 *
 * <p>Agregado raiz da camada social. É o que separa a Corretoria de um marketplace puro: o
 * corretor tem presença — fala sobre o mercado, mostra bastidores — e não apenas um estoque de
 * anúncios.
 *
 * <p>Comentários são dependentes e vivem por cascade; curtidas são junção própria, porque
 * curtir é um vínculo entre pessoa e post, consultado por contagem.
 */
@Getter
@Setter
@Entity
@Table(name = "realtor_posts")
public class RealtorPost extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "realtor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_posts_users"))
    private User realtor;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @BatchSize(size = 50)
    @OrderBy("createdAt ASC")
    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<RealtorPostComment> comments = new ArrayList<>();

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public RealtorPost() {
    }

    public void addComment(RealtorPostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RealtorPost post)) {
            return false;
        }
        return id != null && id.equals(post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
