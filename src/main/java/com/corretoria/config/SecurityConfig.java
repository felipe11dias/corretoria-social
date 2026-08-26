package com.corretoria.config;

import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança: stateless, com JWT e autorização em nível de método.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    /**
     * Leitura pública da Fase 2.
     *
     * <p>Três vitrines: o catálogo de anúncios, o mural do corretor e a reputação dele. As
     * três são o que alguém consulta <em>antes</em> de decidir criar conta — reputação que só
     * o dono enxerga não ajuda ninguém a escolher com quem negociar.
     *
     * <p>A vitrine de <em>procura</em> (buscas salvas públicas) ficou deliberadamente de fora:
     * ela revela o que uma pessoa quer, onde e por quanto.
     */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/listings",
            "/api/v1/listings/*",
            "/api/v1/realtors/*/posts",
            "/api/v1/realtors/*/reviews"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Sem sessão e sem formulário de login, não há cookie de sessão para um site
                // de terceiros forçar o navegador a enviar — que é a premissa do CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Apenas o POST é público: é o autocadastro. GET, PUT e DELETE em
                        // /users exigem token, e o escopo é decidido por @PreAuthorize.
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // As vitrines são leitura livre: qualquer visitante lista e abre um
                        // anúncio, o mural e a reputação sem token. Só a leitura — publicar,
                        // editar, curtir e avaliar continuam exigindo autenticação e posse,
                        // decididas por @PreAuthorize em cada controller.
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
