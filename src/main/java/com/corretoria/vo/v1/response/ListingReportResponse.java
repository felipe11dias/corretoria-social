package com.corretoria.vo.v1.response;

import com.corretoria.enums.ReportReason;
import com.corretoria.enums.ReportStatus;
import java.time.LocalDateTime;

/**
 * Denúncia na fila de moderação.
 *
 * <p>Traz o denunciante identificado: moderação anônima convidaria ao uso da denúncia como
 * arma, e quem decide precisa saber se o mesmo usuário está denunciando o mesmo corretor
 * repetidamente.
 *
 * @param reportCountForListing quantas denúncias este anúncio acumulou — o número que o
 *                              moderador olha em seguida, e que distingue uma reclamação
 *                              isolada de um padrão
 * @param decidedBy             login de quem decidiu, vindo da auditoria; nulo enquanto a
 *                              denúncia está em aberto
 */
public record ListingReportResponse(
        Long id,
        ListingSummaryResponse listing,
        UserSummaryResponse reporter,
        ReportReason reason,
        String detail,
        ReportStatus status,
        long reportCountForListing,
        String decidedBy,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}
