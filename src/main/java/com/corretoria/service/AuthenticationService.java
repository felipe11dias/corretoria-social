package com.corretoria.service;

import com.corretoria.security.JwtService;
import com.corretoria.vo.v1.request.LoginRequest;
import com.corretoria.vo.v1.response.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Valida credenciais e emite o token.
 *
 * <p>Separada do {@code UserService} de propósito: cadastro e política de autenticação mudam
 * por razões diferentes. Fundi-las porque "é tudo usuário" violaria a responsabilidade única.
 *
 * <p>A comparação senha × hash não acontece aqui — é delegada ao {@code AuthenticationManager},
 * que usa o {@code PasswordEncoder} configurado. Este serviço nunca vê a senha em texto plano
 * além do que recebe para repassar, e nunca a registra em log.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Autentica e devolve o token.
     *
     * <p>Credencial inválida sobe como {@code AuthenticationException} e vira {@code 401}
     * padronizado no handler global — este serviço não conhece código de status.
     */
    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        String token = jwtService.generateToken(authentication.getName());

        return AuthResponse.bearer(token, jwtService.expiration());
    }
}
