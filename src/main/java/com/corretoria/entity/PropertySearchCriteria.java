package com.corretoria.entity;

import com.corretoria.enums.PropertyType;
import jakarta.persistence.Column;
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
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Critérios de imóvel de uma busca salva.
 *
 * <p>Dependente da {@link SavedSearch} com identidade compartilhada, mesmo padrão de
 * {@link Property} para o anúncio. Todos os campos são opcionais: ausente significa "qualquer
 * valor", e é isso que permite uma busca salva ampla.
 */
@Getter
@Setter
@Entity
@Table(name = "property_search_criteria")
public class PropertySearchCriteria extends Auditable {

    // Ver Property: com @MapsId a chave primária é a própria coluna de junção.
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "saved_search_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_property_search_criteria_saved_searches"))
    private SavedSearch savedSearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", length = 20)
    private PropertyType propertyType;

    @Column(name = "min_bedrooms")
    private Integer minBedrooms;

    @Column(name = "min_bathrooms")
    private Integer minBathrooms;

    @Column(name = "min_total_area", precision = 12, scale = 2)
    private BigDecimal minTotalArea;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public PropertySearchCriteria() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertySearchCriteria criteria)) {
            return false;
        }
        return id != null && id.equals(criteria.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
