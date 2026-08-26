package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Publicação no mural do corretor. */
public record RealtorPostCreationRequest(

        @NotBlank(message = "O conteúdo da publicação é obrigatório")
        @Size(max = 2000, message = "A publicação deve ter no máximo 2000 caracteres")
        String content) {
}
