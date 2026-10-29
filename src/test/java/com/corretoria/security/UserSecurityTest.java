package com.corretoria.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.corretoria.entity.User;
import com.corretoria.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class UserSecurityTest {

    private static final String LOGIN = "corretor.demo";
    private static final Long OWN_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSecurity userSecurity;

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
    @DisplayName("isSelf")
    class IsSelf {

        @Test
        @DisplayName("isSelf - id do próprio usuário autenticado - verdadeiro")
        void shouldAllowOwnResource() {
            given(userRepository.findByLogin(LOGIN)).willReturn(Optional.of(userWithId(OWN_ID)));

            assertThat(userSecurity.isSelf(OWN_ID, authenticatedAs(LOGIN))).isTrue();
        }

        @Test
        @DisplayName("isSelf - id de outro usuário - falso")
        void shouldDenyForeignResource() {
            // Esta é a barreira contra IDOR: sem ela, qualquer token válido acessaria
            // qualquer usuário trocando o id da URL.
            given(userRepository.findByLogin(LOGIN)).willReturn(Optional.of(userWithId(OWN_ID)));

            assertThat(userSecurity.isSelf(OTHER_ID, authenticatedAs(LOGIN))).isFalse();
        }

        @Test
        @DisplayName("isSelf - autenticação nula - falso, sem consultar o banco")
        void shouldDenyWhenNotAuthenticated() {
            assertThat(userSecurity.isSelf(OWN_ID, null)).isFalse();
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("isSelf - id nulo - falso, sem consultar o banco")
        void shouldDenyWhenIdIsNull() {
            assertThat(userSecurity.isSelf(null, authenticatedAs(LOGIN))).isFalse();
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("isSelf - usuário do token já não existe - falso")
        void shouldDenyWhenUserNoLongerExists() {
            given(userRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

            assertThat(userSecurity.isSelf(OWN_ID, authenticatedAs(LOGIN))).isFalse();
        }

        @Test
        @DisplayName("isSelf - usuário anônimo - falso, sem consultar o banco")
        void shouldDenyAnonymous() {
            // O catálogo de anúncios é público, então o token anônimo passa a chegar aqui em
            // volume: reconhecê-lo antes da consulta evita uma ida ao banco por requisição.
            Authentication anonymous = new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

            assertThat(userSecurity.isSelf(OWN_ID, anonymous)).isFalse();
            then(userRepository).shouldHaveNoInteractions();
        }
    }
}
