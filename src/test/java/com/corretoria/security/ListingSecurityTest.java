package com.corretoria.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.corretoria.entity.User;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.UserRepository;
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
class ListingSecurityTest {

    private static final String LOGIN = "corretor.demo";
    private static final Long LISTING_ID = 10L;
    private static final Long OWN_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingSecurity listingSecurity;

    private static Authentication authenticatedAs(String login) {
        return new UsernamePasswordAuthenticationToken(
                login, null, AuthorityUtils.createAuthorityList("ROLE_REALTOR"));
    }

    private static User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        user.setLogin(LOGIN);
        return user;
    }

    @Nested
    @DisplayName("isOwner")
    class IsOwner {

        @Test
        @DisplayName("isOwner - anúncio do próprio corretor - verdadeiro")
        void shouldAllowOwnListing() {
            given(listingRepository.findRealtorIdById(LISTING_ID)).willReturn(Optional.of(OWN_ID));
            given(userRepository.findByLogin(LOGIN)).willReturn(Optional.of(userWithId(OWN_ID)));

            assertThat(listingSecurity.isOwner(LISTING_ID, authenticatedAs(LOGIN))).isTrue();
        }

        @Test
        @DisplayName("isOwner - anúncio de outro corretor - falso")
        void shouldDenyForeignListing() {
            // Barreira contra IDOR: sem ela, qualquer corretor autenticado editaria, pausaria
            // ou apagaria a foto do anúncio de outro trocando o id da URL.
            given(listingRepository.findRealtorIdById(LISTING_ID))
                    .willReturn(Optional.of(OTHER_ID));
            given(userRepository.findByLogin(LOGIN)).willReturn(Optional.of(userWithId(OWN_ID)));

            assertThat(listingSecurity.isOwner(LISTING_ID, authenticatedAs(LOGIN))).isFalse();
        }

        @Test
        @DisplayName("isOwner - anúncio inexistente - falso, sem consultar o usuário")
        void shouldDenyUnknownListing() {
            given(listingRepository.findRealtorIdById(LISTING_ID)).willReturn(Optional.empty());

            assertThat(listingSecurity.isOwner(LISTING_ID, authenticatedAs(LOGIN))).isFalse();
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("isOwner - autenticação nula - falso, sem consultar o banco")
        void shouldDenyWhenNotAuthenticated() {
            assertThat(listingSecurity.isOwner(LISTING_ID, null)).isFalse();
            then(listingRepository).shouldHaveNoInteractions();
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("isOwner - id de anúncio nulo - falso, sem consultar o banco")
        void shouldDenyWhenListingIdIsNull() {
            assertThat(listingSecurity.isOwner(null, authenticatedAs(LOGIN))).isFalse();
            then(listingRepository).shouldHaveNoInteractions();
        }
    }
}
