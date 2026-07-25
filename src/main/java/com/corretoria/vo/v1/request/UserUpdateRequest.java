package com.corretoria.vo.v1.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Atualização do cadastro.
 *
 * <p>Não carrega senha nem papéis de propósito: a troca de senha tem endpoint próprio
 * ({@code PATCH /users/{id}/password}) e a concessão de papéis é operação do administrador.
 * Aceitá-los aqui abriria caminho para escalonamento de privilégio por uma rota de edição.
 */
public record UserUpdateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "O login é obrigatório")
        @Size(min = 4, max = 255, message = "O login deve ter entre 4 e 255 caracteres")
        String login,

        @Valid
        List<AddressRequest> addresses) {
}
