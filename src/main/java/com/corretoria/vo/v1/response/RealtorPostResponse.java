package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Publicação do mural de um corretor.
 *
 * @param likeCount total de curtidas — contado no banco, e não pelo tamanho de uma lista de
 *                  curtidas que ninguém pediu para ver
 * @param comments  comentários, do mais antigo para o mais recente
 */
public record RealtorPostResponse(
        Long id,
        RealtorSummaryResponse realtor,
        String content,
        long likeCount,
        List<RealtorPostCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}
