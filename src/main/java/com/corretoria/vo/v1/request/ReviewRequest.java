package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Avaliação de um corretor.
 *
 * <p>Um único VO para criar e atualizar, porque a operação é uma só: avaliar de novo o mesmo
 * corretor <strong>substitui</strong> a nota anterior. Ter um {@code ...UpdateRequest}
 * separado sugeriria um histórico de avaliações que não existe.
 *
 * <p>O corretor avaliado vem da rota, e o autor vem do token — nenhum dos dois no corpo.
 */
public record ReviewRequest(

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota vai de 1 a 5")
        @Max(value = 5, message = "A nota vai de 1 a 5")
        Integer score,

        @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
        String comment) {
}
