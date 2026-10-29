package com.corretoria.vo.v1.response;

/**
 * Cliente autor de uma busca pública, na forma resumida que a vitrine de procura expõe.
 *
 * <p>Só id e nome, pelo mesmo motivo de {@link RealtorSummaryResponse}: quem vê a vitrine é
 * qualquer corretor autenticado, e devolver o cadastro inteiro exporia e-mail, login e
 * endereços de quem só quis registrar uma procura.
 *
 * <p>Existe separado de {@code RealtorSummaryResponse} apesar da forma idêntica: corretor e
 * cliente são conceitos distintos no glossário, e um contrato que os confunda hoje é o que
 * impede evoluí-los em separado amanhã.
 */
public record CustomerSummaryResponse(Long id, String name) {
}
