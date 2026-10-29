package com.corretoria.service;

import com.corretoria.entity.ListingFavorite;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.SocialMapper;
import com.corretoria.repository.ListingFavoriteRepository;
import com.corretoria.repository.ListingRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.response.ListingFavoriteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Favoritos do cliente.
 *
 * <p>Marcar e desmarcar são <strong>idempotentes</strong>: favoritar duas vezes deixa um
 * favorito, e desfavoritar o que não estava marcado não é erro. É o comportamento que um botão
 * de alternar precisa — o cliente da API pode repetir a chamada sem consequência, e uma
 * segunda tentativa depois de uma falha de rede não vira {@code 409}.
 */
@Service
public class ListingFavoriteService {

    private final ListingFavoriteRepository listingFavoriteRepository;
    private final ListingRepository listingRepository;
    private final SocialMapper socialMapper;
    private final UserSecurity userSecurity;

    public ListingFavoriteService(
            ListingFavoriteRepository listingFavoriteRepository,
            ListingRepository listingRepository,
            SocialMapper socialMapper,
            UserSecurity userSecurity) {
        this.listingFavoriteRepository = listingFavoriteRepository;
        this.listingRepository = listingRepository;
        this.socialMapper = socialMapper;
        this.userSecurity = userSecurity;
    }

    @Transactional
    public ListingFavoriteResponse favorite(Long listingId, Authentication authentication) {
        User customer = userSecurity.getAuthenticatedUser(authentication);
        ensureCustomerRole(customer);

        ListingFavorite favorite = listingFavoriteRepository
                .findByCustomerIdAndListingId(customer.getId(), listingId)
                .orElseGet(() -> newFavorite(customer, listingId));

        return socialMapper.toResponse(listingFavoriteRepository.save(favorite));
    }

    @Transactional
    public void unfavorite(Long listingId, Authentication authentication) {
        User customer = userSecurity.getAuthenticatedUser(authentication);

        listingFavoriteRepository
                .findByCustomerIdAndListingId(customer.getId(), listingId)
                .ifPresent(listingFavoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<ListingFavoriteResponse> findMine(Authentication authentication, Pageable pageable) {
        User customer = userSecurity.getAuthenticatedUser(authentication);

        return listingFavoriteRepository.findByCustomerId(customer.getId(), pageable)
                .map(socialMapper::toResponse);
    }

    private ListingFavorite newFavorite(User customer, Long listingId) {
        ListingFavorite favorite = new ListingFavorite();
        favorite.setCustomer(customer);
        favorite.setListing(listingRepository.findById(listingId)
                .orElseThrow(() -> ResourceNotFoundException.listing(listingId)));
        return favorite;
    }

    private void ensureCustomerRole(User customer) {
        boolean hasCustomerRole = customer.getRoles().stream()
                .map(Role::getName)
                .anyMatch(RoleName.ROLE_CUSTOMER::equals);

        if (!hasCustomerRole) {
            throw ForbiddenOperationException.customerRoleRequired();
        }
    }
}
