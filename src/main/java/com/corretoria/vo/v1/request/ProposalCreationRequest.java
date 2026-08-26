package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Abertura de uma negociação.
 *
 * <p>O mesmo corpo serve às duas direções, e <strong>quem propõe não é informado</strong>: o
 * papel de quem chama é lido do token e decide o resto. Cliente autenticado propõe sobre o
 * anúncio; corretor autenticado propõe o próprio anúncio a um cliente, e aí
 * {@code savedSearchId} é obrigatório — é a busca pública que justifica a abordagem e diz
 * <em>a quem</em> a proposta se destina.
 *
 * @param savedSearchId busca pública de origem; obrigatório quando quem propõe é o corretor,
 *                      proibido quando é o cliente
 */
public record ProposalCreationRequest(

        @NotNull(message = "O anúncio é obrigatório")
        Long listingId,

        Long savedSearchId,

        @NotNull(message = "O valor da proposta é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "O valor da proposta deve ser maior que zero")
        BigDecimal amount) {
}
