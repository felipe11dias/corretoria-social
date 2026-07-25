package com.corretoria.exception;

/**
 * Operação não permitida pela regra de negócio — traduzida em {@code 403} com
 * {@code /problems/forbidden-operation}.
 *
 * <p>Distinta de {@code AccessDeniedException}, que significa "este recurso não é seu". Aqui
 * a operação é proibida <em>em si</em>: o caso do projeto é o autocadastro público
 * solicitando {@code ROLE_ADMIN}, tentativa de escalonamento de privilégio. Dois erros com o
 * mesmo {@code 403} e {@code type} diferente — que é exatamente o que o RFC 7807 resolve.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public static ForbiddenOperationException selfRegistrationCannotGrantAdmin() {
        return new ForbiddenOperationException(
                "O autocadastro público não concede o papel ROLE_ADMIN");
    }
}
