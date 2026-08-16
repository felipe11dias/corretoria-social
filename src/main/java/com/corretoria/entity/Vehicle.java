package com.corretoria.entity;

import com.corretoria.enums.FuelType;
import com.corretoria.enums.TransmissionType;
import com.corretoria.enums.VehicleCondition;
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
 * Atributos específicos de um anúncio de automóvel.
 *
 * <p>Dependente do {@link Listing} pelo mesmo padrão de {@link Property}: identidade
 * compartilhada, cascade e {@code orphanRemoval}, sem repository próprio.
 *
 * <p>Placa e chassi não são modelados: identificam o bem publicamente e o catálogo é aberto
 * a qualquer visitante. Se vierem a ser necessários para antifraude, entram como dado interno,
 * fora de qualquer {@code Response}.
 */
@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle extends Auditable {

    // Ver Property: com @MapsId a chave primária é a própria coluna de junção.
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehicles_listings"))
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "color", nullable = false)
    private String color;

    @Column(name = "manufacture_year", nullable = false)
    private Integer manufactureYear;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Column(name = "mileage_km", nullable = false)
    private Integer mileageKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission", nullable = false, length = 20)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private VehicleCondition condition;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Vehicle() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vehicle vehicle)) {
            return false;
        }
        return id != null && id.equals(vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
