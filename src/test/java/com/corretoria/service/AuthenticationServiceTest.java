package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.corretoria.security.JwtService;
import com.corretoria.vo.v1.request.LoginRequest;
import com.corretoria.vo.v1.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String LOGIN = "corretor.demo";
    private static final String PASSWORD = "corretor12345";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.fake.token";
    private static final long EXPIRATION = 3_600_000L;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static Authentication authenticated() {
        return new UsernamePasswordAuthenticationToken(
                LOGIN, null, AuthorityUtils.createAuthorityList("ROLE_REALTOR"));
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("authenticate - credenciais válidas - emite o token")
        void shouldIssueTokenForValidCredentials() {
            // arrange
            given(authenticationManager.authenticate(any())).willReturn(authenticated());
            given(jwtService.generateToken(LOGIN)).willReturn(TOKEN);
            given(jwtService.expiration()).willReturn(EXPIRATION);

            // act
            AuthResponse response =
                    authenticationService.authenticate(new LoginRequest(LOGIN, PASSWORD));

            // assert
            assertThat(response.token()).isEqualTo(TOKEN);
            assertThat(response.type()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(EXPIRATION);
        }

        @Test
        @DisplayName("authenticate - credenciais inválidas - propaga AuthenticationException")
        void shouldPropagateAuthenticationFailure() {
            // O serviço não traduz para HTTP: quem transforma em 401 é o handler global.
            willThrow(new BadCredentialsException("Bad credentials"))
                    .given(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authenticationService
                    .authenticate(new LoginRequest(LOGIN, "senha-errada")))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("authenticate - credenciais inválidas - nenhum token é emitido")
        void shouldNotIssueTokenWhenAuthenticationFails() {
            willThrow(new BadCredentialsException("Bad credentials"))
                    .given(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authenticationService
                    .authenticate(new LoginRequest(LOGIN, "senha-errada")))
                    .isInstanceOf(AuthenticationException.class);

            then(jwtService).shouldHaveNoInteractions();
        }
    }
}
