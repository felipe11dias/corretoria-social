package com.corretoria.vo.v1.response;

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import java.math.BigDecimal;

/**
 * Anúncio na forma resumida, para quando ele aparece dentro de outro recurso.
 *
 * <p>Traz o suficiente para identificar o que está sendo negociado — título, preço pedido e
 * situação — sem arrastar descrição, coordenadas, detalhamento e fotos para dentro de cada
 * proposta. Quem quiser o anúncio inteiro segue o link.
 *
 * @param price preço <strong>pedido no anúncio</strong>, que é o ponto de comparação do valor
 *              proposto
 */
public record ListingSummaryResponse(
        Long id,
        ListingType type,
        ListingStatus status,
        String title,
        BigDecimal price) {
}
