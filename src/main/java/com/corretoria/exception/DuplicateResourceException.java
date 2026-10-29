package com.corretoria.exception;

/**
 * Violação de unicidade de e-mail ou login — traduzida em {@code 409} com
 * {@code /problems/duplicate-resource}.
 *
 * <p>A checagem que dispara esta exceção só é correta porque o e-mail é normalizado para
 * minúsculas antes (VO {@code Email}).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("E-mail já cadastrado: " + email);
    }

    public static DuplicateResourceException login(String login) {
        return new DuplicateResourceException("Login já cadastrado: " + login);
    }

    /**
     * Segunda denúncia aberta da mesma pessoa sobre o mesmo anúncio.
     *
     * <p>Conflito, e não erro de validação: a primeira denúncia existe e está na fila. Depois
     * de decidida, uma nova é legítima — o anúncio pode ter mudado e voltado a ter problema.
     */
    public static DuplicateResourceException openListingReport(Long listingId) {
        return new DuplicateResourceException(
                "Você já tem uma denúncia em aberto para o anúncio " + listingId);
    }
}
