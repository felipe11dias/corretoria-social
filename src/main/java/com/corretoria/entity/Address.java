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
 * Endereço de um usuário.
 *
 * <p>Não é agregado raiz: nasce, muda e morre junto do {@link User}, por cascade e
 * {@code orphanRemoval} — por isso não possui repository próprio.
 */
@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_addresses_users"))
    private User user;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "number")
    private String number;

    @Column(name = "complement")
    private String complement;

    @Column(name = "neighborhood")
    private String neighborhood;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    /** Guardado com 8 dígitos, sem máscara: a formatação é responsabilidade do VO de saída. */
    @Column(name = "zip_code", nullable = false, length = 9)
    private String zipCode;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Address() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Address address)) {
            return false;
        }
        return id != null && id.equals(address.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
