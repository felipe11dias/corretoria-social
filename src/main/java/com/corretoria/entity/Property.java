package com.corretoria.entity;

import com.corretoria.enums.PropertyAmenity;
import com.corretoria.enums.PropertyType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Atributos específicos de um anúncio de imóvel.
 *
 * <p>Não é agregado raiz: nasce, muda e morre junto do {@link Listing}, por cascade e
 * {@code orphanRemoval} — mesmo padrão de {@link Address}, por isso sem repository próprio.
 *
 * <p>A chave primária é a do próprio anúncio ({@code @MapsId}), e não uma sequência
 * independente: um imóvel não existe fora do anúncio que o descreve, e a identidade
 * compartilhada é o que impede dois detalhamentos para o mesmo {@code Listing}.
 */
@Getter
@Setter
@Entity
@Table(name = "properties")
public class Property extends Auditable {

    // Sem @Column: com @MapsId a coluna da chave primária é a própria coluna de junção
    // (listing_id), declarada uma única vez no @JoinColumn abaixo.
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_properties_listings"))
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    /** Área total em metros quadrados. */
    @Column(name = "total_area", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "garage_spots")
    private Integer garageSpots;

    @Column(name = "condominium_fee", precision = 15, scale = 2)
    private BigDecimal condominiumFee;

    @Column(name = "iptu", precision = 15, scale = 2)
    private BigDecimal iptu;

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(
                    name = "listing_id",
                    foreignKey = @ForeignKey(name = "fk_property_amenities_properties")))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", nullable = false, length = 30)
    private Set<PropertyAmenity> amenities = new HashSet<>();

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Property() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Property property)) {
            return false;
        }
        return id != null && id.equals(property.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
