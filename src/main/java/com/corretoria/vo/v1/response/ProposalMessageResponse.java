package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;

/**
 * Mensagem no thread de uma proposta.
 *
 * <p>O autor sai resumido porque pode ser qualquer um dos dois lados — e quem lê o thread já
 * sabe quem são os participantes.
 */
public record ProposalMessageResponse(
        Long id,
        UserSummaryResponse author,
        String content,
        LocalDateTime createdAt) {
}
