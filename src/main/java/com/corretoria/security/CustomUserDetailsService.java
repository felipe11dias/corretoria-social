package com.corretoria.security;

import com.corretoria.entity.Role;
import com.corretoria.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o usuário para o Spring Security e converte seus papéis em autoridades.
 *
 * <p>A entidade {@code User} <strong>não</strong> implementa {@code UserDetails}: fazê-la
 * implementar arrastaria o domínio para dentro do framework de segurança. Aqui ela é
 * traduzida para o {@code UserDetails} do próprio Spring, mantendo a entidade limpa.
 *
 * <p>As constantes de {@code RoleName} já vêm com o prefixo {@code ROLE_} exigido pelo
 * Spring, e por isso são usadas como autoridade sem transformação.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        return userRepository.findByLogin(login)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }

    private UserDetails toUserDetails(com.corretoria.entity.User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
