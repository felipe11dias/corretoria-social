package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca da própria senha.
 *
 * <p>A senha atual é exigida mesmo com o usuário já autenticado: um token vazado não deve
 * bastar para trocar a senha e tomar a conta.
 *
 * <p>A coincidência entre {@code newPassword} e {@code confirmPassword} é verificada no
 * serviço, e não por Bean Validation, porque a divergência é regra de negócio — responde
 * {@code InvalidPasswordException} (400, {@code /problems/invalid-password}), não erro de
 * formato de campo.
 */
public record PasswordChangeRequest(

        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres")
        String newPassword,

        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        String confirmPassword) {
}
