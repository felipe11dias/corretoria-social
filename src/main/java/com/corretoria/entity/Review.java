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
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Avaliação de um corretor por um cliente.
 *
 * <p>Agregado raiz. A reputação só é útil se for difícil de fabricar: avaliar exige ter havido
 * ao menos uma {@link Proposal} entre as duas partes — o análogo da "compra verificada". A
 * elegibilidade é conferida no serviço, no momento da criação, e a proposta que a habilitou
 * não fica guardada: o que importa é ter existido negociação, não qual.
 *
 * <p>Uma avaliação por par corretor/cliente, garantida por {@code UNIQUE}: avaliar de novo
 * <strong>atualiza</strong> a nota em vez de acumular várias. Sem isso, um cliente sozinho
 * moveria a média do corretor repetindo a avaliação.
 */
@Getter
@Setter
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reviews_realtor_id_customer_id",
                columnNames = {"realtor_id", "customer_id"}))
public class Review extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Sujeito da avaliação. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "realtor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_realtors"))
    private User realtor;

    /** Autor da avaliação. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_customers"))
    private User customer;

    /** Nota de 1 a 5, com o intervalo garantido também por CHECK no banco. */
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comment", length = 2000)
    private String comment;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Review() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Review review)) {
            return false;
        }
        return id != null && id.equals(review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
