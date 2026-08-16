package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Identidade e posse do usuário autenticado.
 *
 * <p>Responde a duas perguntas da mesma natureza: <em>quem</em> está autenticado e se o recurso
 * pedido é <em>dele</em>. Usada em SpEL nos controllers
 * ({@code @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)")}) e
 * pelos serviços que precisam do dono da operação.
 *
 * <p>Sem esta verificação, qualquer token válido leria, alteraria ou excluiria <em>qualquer</em>
 * usuário pelo id da URL — a falha conhecida como IDOR (referência direta insegura a objeto).
 * A identidade vem sempre do token, nunca do corpo da requisição.
 */
@Component
public class UserSecurity {

    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSelf(Long id, Authentication authentication) {
        if (id == null) {
            return false;
        }

        return findAuthenticatedUser(authentication)
                .map(User::getId)
                .filter(id::equals)
                .isPresent();
    }

    /** Usuário do token, ou vazio quando a requisição é anônima. */
    public Optional<User> findAuthenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return userRepository.findByLogin(authentication.getName());
    }

    /**
     * Usuário do token, exigindo que ele exista.
     *
     * <p>O token pode ser válido e o usuário ter sido excluído no meio da validade — daí o
     * {@code 404} em vez de deixar um {@code NullPointerException} virar {@code 500}.
     */
    public User getAuthenticatedUser(Authentication authentication) {
        return findAuthenticatedUser(authentication)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário autenticado não encontrado"));
    }

    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, RoleName.ROLE_ADMIN);
    }

    private boolean hasRole(Authentication authentication, RoleName role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role.name()::equals);
    }
}
