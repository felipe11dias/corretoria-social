package com.corretoria.entity;

import com.corretoria.enums.ProposalInitiator;
import com.corretoria.enums.ProposalStatus;
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
 * Proposta sobre um anúncio.
 *
 * <p>Agregado raiz da negociação. É <strong>bidirecional</strong>: pode nascer do cliente,
 * sobre um anúncio que ele viu, ou do corretor, a partir de uma busca pública de um cliente —
 * o mercado reverso da plataforma. Quem começou fica registrado em {@link ProposalInitiator}.
 *
 * <p>O campo {@code customer} é <strong>sempre</strong> a contraparte cliente, independente de
 * quem iniciou; o corretor da negociação é o dono do anúncio. Guardar os dois papéis por
 * função, e não por "quem propôs" e "quem recebeu", é o que mantém a consulta de "minhas
 * propostas" simples nos dois lados.
 *
 * <p>Não há estado de contraproposta: contrapor é mandar uma {@link ProposalMessage} sugerindo
 * outro valor.
 */
@Getter
@Setter
@Entity
@Table(name = "proposals")
public class Proposal extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_proposals_listings"))
    private Listing listing;

    /** Contraparte cliente, sempre — mesmo quando foi o corretor que iniciou. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_proposals_users"))
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator", nullable = false, length = 20)
    private ProposalInitiator initiator;

    /**
     * Busca pública que originou a abordagem, quando o corretor tomou a iniciativa.
     *
     * <p>Nula na direção oposta. Serve de rastro: sem ela, o cliente receberia uma proposta
     * sem saber por que aquele corretor o procurou.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "origin_saved_search_id",
            foreignKey = @ForeignKey(name = "fk_proposals_saved_searches"))
    private SavedSearch originSavedSearch;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.PENDING;

    @BatchSize(size = 50)
    @OrderBy("createdAt ASC")
    @OneToMany(
            mappedBy = "proposal",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ProposalMessage> messages = new ArrayList<>();

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public Proposal() {
    }

    public void addMessage(ProposalMessage message) {
        messages.add(message);
        message.setProposal(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Proposal proposal)) {
            return false;
        }
        return id != null && id.equals(proposal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
