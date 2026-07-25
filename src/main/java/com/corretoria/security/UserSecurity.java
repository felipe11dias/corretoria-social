package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por posse: decide se o recurso pedido pertence a quem está autenticado.
 *
 * <p>Usada em SpEL nos controllers:
 * {@code @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)")}.
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
        if (id == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return userRepository.findByLogin(authentication.getName())
                .map(User::getId)
                .filter(id::equals)
                .isPresent();
    }
}
