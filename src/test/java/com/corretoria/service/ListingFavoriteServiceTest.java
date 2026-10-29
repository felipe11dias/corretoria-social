package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Listing;
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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class ListingFavoriteServiceTest {

    private static final Long LISTING_ID = 10L;
    private static final Long CUSTOMER_ID = 2L;

    @Mock
    private ListingFavoriteRepository listingFavoriteRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private SocialMapper socialMapper;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private ListingFavoriteService listingFavoriteService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "cliente.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser(RoleName... roles) {
        User user = new User();
        user.setId(CUSTOMER_ID);
        for (RoleName role : roles) {
            user.addRole(new Role(role));
        }
        return user;
    }

    @Nested
    @DisplayName("favorite")
    class Favorite {

        @Test
        @DisplayName("favorite - anúncio ainda não marcado - cria o vínculo")
        void shouldCreateFavorite() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));
            given(listingFavoriteRepository.findByCustomerIdAndListingId(CUSTOMER_ID, LISTING_ID))
                    .willReturn(Optional.empty());
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(new Listing()));
            given(listingFavoriteRepository.save(any())).willAnswer(call -> call.getArgument(0));

            listingFavoriteService.favorite(LISTING_ID, authentication);

            then(listingFavoriteRepository).should().save(any(ListingFavorite.class));
        }

        @Test
        @DisplayName("favorite - já marcado - reaproveita o vínculo, sem duplicar")
        void shouldBeIdempotent() {
            // Um botão de alternar precisa disso: repetir a chamada após falha de rede não
            // pode virar 409.
            Authentication authentication = anAuthentication();
            ListingFavorite existing = new ListingFavorite();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));
            given(listingFavoriteRepository.findByCustomerIdAndListingId(CUSTOMER_ID, LISTING_ID))
                    .willReturn(Optional.of(existing));
            given(listingFavoriteRepository.save(existing)).willReturn(existing);

            listingFavoriteService.favorite(LISTING_ID, authentication);

            then(listingRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("favorite - corretor - lança ForbiddenOperationException")
        void shouldRejectNonCustomer() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_REALTOR));

            assertThatThrownBy(() ->
                    listingFavoriteService.favorite(LISTING_ID, authentication))
                    .isInstanceOf(ForbiddenOperationException.class);

            then(listingFavoriteRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("favorite - anúncio inexistente - lança ResourceNotFoundException")
        void shouldRejectUnknownListing() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));
            given(listingFavoriteRepository.findByCustomerIdAndListingId(CUSTOMER_ID, LISTING_ID))
                    .willReturn(Optional.empty());
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    listingFavoriteService.favorite(LISTING_ID, authentication))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unfavorite")
    class Unfavorite {

        @Test
        @DisplayName("unfavorite - não estava marcado - não é erro")
        void shouldTolerateMissingFavorite() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));
            given(listingFavoriteRepository.findByCustomerIdAndListingId(CUSTOMER_ID, LISTING_ID))
                    .willReturn(Optional.empty());

            listingFavoriteService.unfavorite(LISTING_ID, authentication);

            then(listingFavoriteRepository).should(never()).delete(any());
        }
    }
}
