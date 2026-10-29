package com.corretoria.vo.v1.response;

/**
 * Corretor responsável por um anúncio, na forma resumida que o catálogo expõe.
 *
 * <p>Só id e nome: o catálogo é público, e devolver o {@code UserResponse} inteiro exporia
 * e-mail, login e endereços do corretor a qualquer visitante. Quem precisa do cadastro
 * completo consulta {@code /users/{id}}, que exige token e posse.
 */
public record RealtorSummaryResponse(Long id, String name) {
}
