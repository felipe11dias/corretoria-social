package com.corretoria.enums;

/**
 * Situação de uma proposta.
 *
 * <p>Não existe estado de contraproposta: contrapor é mandar uma mensagem no thread sugerindo
 * outro valor. Formalizar cada ida e volta como estado transformaria a negociação em máquina
 * de estados sem ganho — o valor acordado é o da proposta aceita.
 *
 * <p>{@code PENDING} é o único estado do qual se sai: aceitar, recusar e retirar são
 * definitivos, e uma nova tentativa é uma proposta nova.
 */
public enum ProposalStatus {

    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}
