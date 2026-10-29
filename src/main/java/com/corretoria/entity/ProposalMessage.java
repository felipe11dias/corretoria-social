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
 * Mensagem no thread de uma proposta.
 *
 * <p>Dependente da {@link Proposal}: existe dentro de uma negociação concreta, e não como chat
 * livre entre usuários. A conversa fica ancorada no que está sendo negociado — é o que evita
 * transformar a plataforma em mensageiro e mantém o histórico junto do valor proposto.
 *
 * <p>O {@code createdAt} herdado de {@link Auditable} é o carimbo de tempo da mensagem; não há
 * campo separado para isso.
 */
@Getter
@Setter
@Entity
@Table(name = "proposal_messages")
public class ProposalMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "proposal_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_proposal_messages_proposals"))
    private Proposal proposal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_proposal_messages_users"))
    private User author;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public ProposalMessage() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProposalMessage message)) {
            return false;
        }
        return id != null && id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
