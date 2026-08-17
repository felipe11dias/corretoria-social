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

    public static ResourceNotFoundException listing(Long id) {
        return new ResourceNotFoundException("Anúncio não encontrado: " + id);
    }

    public static ResourceNotFoundException listingPhoto(Long id) {
        return new ResourceNotFoundException("Foto não encontrada: " + id);
    }

    public static ResourceNotFoundException savedSearch(Long id) {
        return new ResourceNotFoundException("Busca salva não encontrada: " + id);
    }

    public static ResourceNotFoundException notification(Long id) {
        return new ResourceNotFoundException("Notificação não encontrada: " + id);
    }
}
