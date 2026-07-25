package com.corretoria.vo.v1.request;

import com.corretoria.enums.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * Autocadastro de corretor ou cliente.
 *
 * <p>O campo {@code roles} é aceito no contrato, mas o serviço <strong>rejeita</strong>
 * solicitações que incluam {@code ROLE_ADMIN} — o autocadastro é público e não pode conceder
 * papel privilegiado.
 */
public record UserRegistrationRequest(

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

        // O limite superior é do BCrypt: ele ignora o que passa de 72 bytes.
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String password,

        @NotEmpty(message = "Informe ao menos um papel")
        Set<RoleName> roles,

        @Valid
        List<AddressRequest> addresses) {
}
