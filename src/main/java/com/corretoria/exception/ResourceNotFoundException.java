package com.corretoria.exception;

/**
 * Recurso inexistente — traduzida em {@code 404} com
 * {@code /problems/resource-not-found}.
 *
 * <p>A mensagem inclui o identificador procurado para tornar o log útil, mas nunca dado
 * sensível.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException("Usuário não encontrado: " + id);
    }

    public static ResourceNotFoundException role(Object name) {
        return new ResourceNotFoundException("Papel não encontrado: " + name);
    }
}
