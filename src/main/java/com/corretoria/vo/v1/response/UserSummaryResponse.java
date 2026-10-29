package com.corretoria.vo.v1.response;

/**
 * Usuário na forma resumida, quando o papel dele não é o ponto.
 *
 * <p>Usada onde o mesmo campo pode trazer um corretor ou um cliente — o autor de uma mensagem
 * no thread de uma proposta, por exemplo. Onde o papel importa, o contrato é específico
 * ({@link RealtorSummaryResponse}, {@link CustomerSummaryResponse}), porque é o papel que
 * explica o que aquela pessoa está fazendo ali.
 */
public record UserSummaryResponse(Long id, String name) {
}
