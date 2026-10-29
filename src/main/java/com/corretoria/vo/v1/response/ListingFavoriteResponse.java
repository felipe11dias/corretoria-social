package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;

/**
 * Anúncio favoritado.
 *
 * <p>Traz o anúncio resumido e a data em que foi marcado: é a lista "meus favoritos", e quem a
 * abre quer reconhecer os anúncios, não reler cada um por inteiro.
 */
public record ListingFavoriteResponse(
        Long id,
        ListingSummaryResponse listing,
        LocalDateTime createdAt) {
}
