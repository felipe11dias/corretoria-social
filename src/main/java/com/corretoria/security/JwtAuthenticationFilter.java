package com.corretoria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica cada requisição a partir do cabeçalho {@code Authorization: Bearer <token>}.
 *
 * <p>O filtro nunca rejeita a requisição por conta própria: quando não há token, ou o token é
 * inválido, ele simplesmente <strong>não autentica</strong> e segue a cadeia. Quem decide se
 * aquele recurso exigia autenticação é a configuração da filter chain, e a resposta é
 * montada pelo {@link JwtAuthenticationEntryPoint}. Sem essa divisão, o filtro devolveria
 * {@code 401} até nos endpoints públicos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        bearerTokenOf(request)
                .flatMap(jwtService::extractLogin)
                .ifPresent(login -> authenticate(login, request));

        filterChain.doFilter(request, response);
    }

    private Optional<String> bearerTokenOf(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }

    private void authenticate(String login, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(login);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException exception) {
            // Token assinado por nós, mas de um usuário que já não existe: segue sem
            // autenticar, e a filter chain devolve 401 se o recurso exigir autenticação.
            SecurityContextHolder.clearContext();
        }
    }
}
