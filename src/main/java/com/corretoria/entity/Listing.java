package com.corretoria.entity;

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Anúncio publicado por um corretor.
 *
 * <p>Agregado raiz do catálogo. É deliberadamente único para os dois domínios: o que muda
 * entre imóvel e automóvel são os atributos específicos, que vivem em {@link Property} ou
 * {@link Vehicle} — nunca nos dois ao mesmo tempo. Preço, localização, situação e autoria são
 * comuns e ficam aqui, o que permite uma única consulta de catálogo para o que o usuário
 * percebe como uma única busca.
 *
 * <p>{@code createdAt} (de {@link Auditable}) é também a data de publicação: publicar é
 * imediato, sem fila de aprovação.
 */
@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "realtor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listings_users"))
    private User realtor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "neighborhood", nullable = false)
    private String neighborhood;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    /** Coordenada do imóvel/ponto de encontro; habilita a busca por raio. */
    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @OneToOne(
            mappedBy = "listing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Property property;

    @OneToOne(
            mappedBy = "listing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Vehicle vehicle;

    // @BatchSize pelo mesmo motivo de User.roles: na listagem paginada do catálogo, uma
    // consulta de fotos por anúncio seria N+1, e "join fetch" sobre coleção obrigaria o
    // Hibernate a paginar em memória.
    @BatchSize(size = 50)
    @OrderBy("position ASC")
    @OneToMany(
            mappedBy = "listing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ListingPhoto> photos = new ArrayList<>();

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Listing() {
    }

    /**
     * Substitui o detalhamento de imóvel mantendo a referência de volta consistente.
     *
     * <p>Escrito à mão em vez de gerado pelo Lombok porque {@link Property} identifica-se pelo
     * próprio {@code listing} ({@code @MapsId}): sem fechar a referência, o Hibernate não teria
     * de onde tirar a chave primária do detalhamento.
     */
    public void setProperty(Property property) {
        this.property = property;
        if (property != null) {
            property.setListing(this);
        }
    }

    /** Ver {@link #setProperty(Property)}. */
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        if (vehicle != null) {
            vehicle.setListing(this);
        }
    }

    public void addPhoto(ListingPhoto photo) {
        photos.add(photo);
        photo.setListing(this);
    }

    public void removePhoto(ListingPhoto photo) {
        photos.remove(photo);
        photo.setListing(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Listing listing)) {
            return false;
        }
        return id != null && id.equals(listing.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
