package com.corretoria.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolve <em>quem</em> está escrevendo, para as colunas {@code created_by} e
 * {@code last_updated_by}.
 *
 * <p>Lê o login do usuário autenticado no contexto de segurança — mesma fonte usada pela
 * autorização por posse. Quando não há usuário autenticado (autocadastro público, seeds SQL),
 * a escrita é atribuída a {@code system}: a coluna nunca fica nula e a origem fica explícita.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_AUDITOR = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_AUDITOR);
        }

        return Optional.of(authentication.getName());
    }
}
