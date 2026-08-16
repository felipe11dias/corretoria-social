package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por posse do anúncio: decide se o anúncio pedido é de quem está autenticado.
 *
 * <p>Mesmo papel do {@code UserSecurity.isSelf} para o cadastro, aplicado ao catálogo: sem
 * ela, qualquer corretor autenticado editaria, pausaria ou apagaria a foto do anúncio de outro
 * trocando o id da URL.
 */
@Component
public class ListingSecurity {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingSecurity(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    public boolean isOwner(Long listingId, Authentication authentication) {
        if (listingId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return listingRepository.findRealtorIdById(listingId)
                .flatMap(realtorId -> userRepository.findByLogin(authentication.getName())
                        .map(User::getId)
                        .filter(realtorId::equals))
                .isPresent();
    }
}
