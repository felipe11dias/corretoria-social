package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais de autenticação.
 *
 * <p>Sem {@code @Size} nem {@code @Pattern}: restringir o formato aqui devolveria {@code 400}
 * para uma credencial malformada e {@code 401} para uma incorreta, permitindo distinguir uma
 * da outra. Toda credencial inválida deve responder igual.
 */
public record LoginRequest(

        @NotBlank(message = "O login é obrigatório")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        String password) {
}
