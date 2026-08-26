package com.corretoria.enums;

/**
 * Quem deu o primeiro passo na negociação.
 *
 * <p>Registrar a direção importa porque a proposta é bidirecional: o cliente propõe sobre um
 * anúncio, e o corretor propõe a partir de uma busca pública. Além de contar a história, é o
 * que define quem pode retirar a proposta e quem pode aceitá-la — retirar é do iniciador,
 * aceitar é da contraparte.
 */
public enum ProposalInitiator {

    CUSTOMER,
    REALTOR
}
