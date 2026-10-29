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
 * Anúncio marcado como favorito por um cliente.
 *
 * <p>Junção pura: não estende {@link Auditable} porque favorito não é <em>atualizado</em> — só
 * criado e removido. Guardar "quem alterou por último" num vínculo que não muda seria carregar
 * quatro colunas sem uso, o mesmo raciocínio de {@code user_roles}.
 *
 * <p>Tem repository próprio, ao contrário de {@code user_roles}, porque é consultado
 * diretamente: "meus favoritos" é uma lista que o cliente abre.
 */
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "listing_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_listing_favorites_customer_id_listing_id",
                columnNames = {"customer_id", "listing_id"}))
public class ListingFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_favorites_users"))
    private User customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_favorites_listings"))
    private Listing listing;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public ListingFavorite() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListingFavorite favorite)) {
            return false;
        }
        return id != null && id.equals(favorite.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
