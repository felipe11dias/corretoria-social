package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Comentário em uma publicação do mural. */
public record RealtorPostCommentCreationRequest(

        @NotBlank(message = "O conteúdo do comentário é obrigatório")
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres")
        String content) {
}
