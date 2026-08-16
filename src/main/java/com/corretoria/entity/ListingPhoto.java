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
 * Foto de um anúncio.
 *
 * <p>Guarda apenas a <strong>chave do objeto</strong> no storage, nunca a URL completa: o
 * endereço público muda com o ambiente (MinIO local, S3 em produção) e gravá-lo tornaria as
 * linhas inválidas na primeira migração de infraestrutura.
 */
@Getter
@Setter
@Entity
@Table(name = "listing_photos")
public class ListingPhoto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_photos_listings"))
    private Listing listing;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    /** Ordem de exibição na galeria, começando em 1. */
    @Column(name = "position", nullable = false)
    private Integer position;

    /** Foto de capa: a que representa o anúncio na listagem. Uma por anúncio. */
    @Column(name = "cover", nullable = false)
    private boolean cover;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public ListingPhoto() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListingPhoto photo)) {
            return false;
        }
        return id != null && id.equals(photo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
