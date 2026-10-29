package com.corretoria.entity;

import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.SearchVisibility;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Busca salva de um cliente.
 *
 * <p>Agregado raiz. Serve a dois usos com a mesma estrutura, distinguidos por
 * {@link SearchVisibility}: privada, é um alerta — anúncio novo compatível gera notificação
 * para o dono; pública, é também um <em>anúncio de procura</em>, visível aos corretores, que
 * podem propor a partir dela. Os critérios e o casamento são idênticos nos dois casos, e é por
 * isso que não são duas entidades.
 *
 * <p>{@code active} pausa o alerta sem apagar os critérios — o equivalente ao
 * {@code PAUSED} do anúncio.
 */
@Getter
@Setter
@Entity
@Table(name = "saved_searches")
public class SavedSearch extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_saved_searches_users"))
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private SearchVisibility visibility = SearchVisibility.PRIVATE;

    /** Texto exibido na vitrine de procura quando a busca é pública. */
    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 20)
    private OperationType operationType;

    @Column(name = "min_price", precision = 15, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 15, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "city")
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "neighborhood")
    private String neighborhood;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "radius_km")
    private Integer radiusKm;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToOne(
            mappedBy = "savedSearch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private PropertySearchCriteria propertyCriteria;

    @OneToOne(
            mappedBy = "savedSearch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private VehicleSearchCriteria vehicleCriteria;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public SavedSearch() {
    }

    /** Ver {@link Listing#setProperty(Property)}: fecha a referência exigida pelo {@code @MapsId}. */
    public void setPropertyCriteria(PropertySearchCriteria propertyCriteria) {
        this.propertyCriteria = propertyCriteria;
        if (propertyCriteria != null) {
            propertyCriteria.setSavedSearch(this);
        }
    }

    /** Ver {@link #setPropertyCriteria(PropertySearchCriteria)}. */
    public void setVehicleCriteria(VehicleSearchCriteria vehicleCriteria) {
        this.vehicleCriteria = vehicleCriteria;
        if (vehicleCriteria != null) {
            vehicleCriteria.setSavedSearch(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SavedSearch savedSearch)) {
            return false;
        }
        return id != null && id.equals(savedSearch.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
