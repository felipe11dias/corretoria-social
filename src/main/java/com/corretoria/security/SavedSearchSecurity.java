package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.SavedSearchRepository;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por posse da busca salva.
 *
 * <p>Mesmo papel do {@code ListingSecurity} para o anúncio. Aqui o risco é maior que o de
 * edição indevida: uma busca privada revela o que a pessoa procura e onde, e sem esta barreira
 * qualquer usuário autenticado leria a de qualquer outro pelo id da URL.
 */
@Component
public class SavedSearchSecurity {

    private final SavedSearchRepository savedSearchRepository;
    private final UserRepository userRepository;

    public SavedSearchSecurity(
            SavedSearchRepository savedSearchRepository, UserRepository userRepository) {
        this.savedSearchRepository = savedSearchRepository;
        this.userRepository = userRepository;
    }

    public boolean isOwner(Long savedSearchId, Authentication authentication) {
        if (savedSearchId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return savedSearchRepository.findCustomerIdById(savedSearchId)
                .flatMap(customerId -> userRepository.findByLogin(authentication.getName())
                        .map(User::getId)
                        .filter(customerId::equals))
                .isPresent();
    }
}
