package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mensagem no thread de uma proposta.
 *
 * <p>É também o mecanismo de contraproposta: sugerir outro valor é escrever aqui, e não mudar
 * o estado da proposta.
 */
public record ProposalMessageCreationRequest(

        @NotBlank(message = "O conteúdo da mensagem é obrigatório")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
        String content) {
}
