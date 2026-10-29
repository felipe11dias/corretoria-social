package com.corretoria.entity;

import com.corretoria.enums.VehicleType;
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
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Critérios de automóvel de uma busca salva.
 *
 * <p>Dependente da {@link SavedSearch} com identidade compartilhada, mesmo padrão de
 * {@link PropertySearchCriteria}. Todos os campos são opcionais.
 */
@Getter
@Setter
@Entity
@Table(name = "vehicle_search_criteria")
public class VehicleSearchCriteria extends Auditable {

    // Ver Property: com @MapsId a chave primária é a própria coluna de junção.
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "saved_search_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehicle_search_criteria_saved_searches"))
    private SavedSearch savedSearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    /** Comparada com a marca do anúncio sem diferenciar maiúsculas, na própria consulta. */
    @Column(name = "brand")
    private String brand;

    @Column(name = "max_mileage_km")
    private Integer maxMileageKm;

    @Column(name = "min_manufacture_year")
    private Integer minManufactureYear;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public VehicleSearchCriteria() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VehicleSearchCriteria criteria)) {
            return false;
        }
        return id != null && id.equals(criteria.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
