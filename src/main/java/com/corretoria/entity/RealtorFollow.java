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
 * Cliente que acompanha um corretor.
 *
 * <p>Junção pura, pelo mesmo raciocínio de {@link ListingFavorite}: seguir não é atualizado, é
 * criado e desfeito. Com repository próprio, porque a lista de quem se segue é consultada
 * diretamente.
 */
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "realtor_follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_realtor_follows_customer_id_realtor_id",
                columnNames = {"customer_id", "realtor_id"}))
public class RealtorFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_follows_customers"))
    private User customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "realtor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_realtor_follows_realtors"))
    private User realtor;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public RealtorFollow() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RealtorFollow follow)) {
            return false;
        }
        return id != null && id.equals(follow.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
