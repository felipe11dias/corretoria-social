package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;

/**
 * Avaliação de um corretor.
 *
 * <p>O autor sai resumido: quem lê a reputação precisa saber que a avaliação tem dono — é o
 * que a torna verificável —, não o cadastro dele.
 *
 * @param lastUpdatedAt data da última revisão da nota; igual a {@code createdAt} enquanto o
 *                      cliente não tiver mudado de opinião
 */
public record ReviewResponse(
        Long id,
        CustomerSummaryResponse customer,
        Integer score,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}
