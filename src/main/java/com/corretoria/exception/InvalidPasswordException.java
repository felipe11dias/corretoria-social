package com.corretoria.exception;

/**
 * Senha atual incorreta ou confirmação divergente — traduzida em {@code 400} com
 * {@code /problems/invalid-password}.
 *
 * <p>A mensagem <strong>nunca</strong> repete a senha informada, nem em log.
 */
public class InvalidPasswordException extends RuntimeException {

    private static final String CURRENT_PASSWORD_DOES_NOT_MATCH = "A senha atual está incorreta";
    private static final String CONFIRMATION_DOES_NOT_MATCH =
            "A nova senha e a confirmação não coincidem";

    public InvalidPasswordException(String message) {
        super(message);
    }

    public static InvalidPasswordException currentPasswordDoesNotMatch() {
        return new InvalidPasswordException(CURRENT_PASSWORD_DOES_NOT_MATCH);
    }

    public static InvalidPasswordException confirmationDoesNotMatch() {
        return new InvalidPasswordException(CONFIRMATION_DOES_NOT_MATCH);
    }
}
