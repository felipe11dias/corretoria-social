package com.corretoria.vo.v1.request;

import com.corretoria.enums.ListingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Mudança de situação do anúncio.
 *
 * <p>Endpoint próprio, separado da edição, pelo mesmo motivo da troca de senha do usuário:
 * pausar, remover ou marcar como concluído é uma transição de estado, não uma edição de
 * conteúdo.
 */
public record ListingStatusUpdateRequest(

        @NotNull(message = "A situação do anúncio é obrigatória")
        ListingStatus status) {
}
