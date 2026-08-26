package com.corretoria.vo.v1.response;

import com.corretoria.enums.ProposalInitiator;
import com.corretoria.enums.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representação de uma negociação.
 *
 * <p>Traz os dois lados nomeados por papel — {@code customer} e {@code realtor} —, e não por
 * "quem propôs": quem propôs está em {@code initiator}. Assim a mesma resposta é legível pelos
 * dois participantes sem que nenhum precise descobrir de que lado está.
 *
 * @param originSavedSearchId busca pública que originou a abordagem do corretor; nulo quando
 *                            foi o cliente que procurou o anúncio
 * @param messages            thread completo, do mais antigo para o mais recente
 */
public record ProposalResponse(
        Long id,
        ListingSummaryResponse listing,
        CustomerSummaryResponse customer,
        RealtorSummaryResponse realtor,
        ProposalInitiator initiator,
        ProposalStatus status,
        BigDecimal amount,
        Long originSavedSearchId,
        List<ProposalMessageResponse> messages,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {

    /**
     * A negociação ainda está aberta?
     *
     * <p>Derivado aqui, no contrato, e não em quem monta os links: a camada de apresentação
     * não conhece enum de domínio — foi o {@code ArchitectureTest} que barrou a primeira
     * versão, em que o assembler comparava com {@code ProposalStatus.PENDING}.
     */
    public boolean pending() {
        return status == ProposalStatus.PENDING;
    }
}
