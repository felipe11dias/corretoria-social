package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Representação pública de um usuário.
 *
 * <p>Nunca carrega senha nem hash — a regra é verificada no build pela regra de arquitetura
 * {@code responseNaoExpoeDadoSensivel}.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String login,
        Set<RoleResponse> roles,
        List<AddressResponse> addresses,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}
