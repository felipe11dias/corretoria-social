package com.corretoria.vo.v1.request;

import com.corretoria.enums.ProposalStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Desfecho de uma proposta.
 *
 * <p>Aceitar e recusar são da contraparte; retirar é de quem propôs. Quem pode fazer o quê é
 * decidido no serviço, a partir do papel de quem chama e de quem iniciou a negociação.
 */
public record ProposalStatusUpdateRequest(

        @NotNull(message = "A situação da proposta é obrigatória")
        ProposalStatus status) {
}
