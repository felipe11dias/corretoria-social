package com.corretoria.vo.v1.request;

import com.corretoria.enums.ReportStatus;

/**
 * Filtro da fila de moderação, vindo da query string.
 *
 * <p>Existe por duas razões, e a segunda é arquitetural: além de dar nome ao critério, mantém o
 * enum de domínio fora da assinatura do controller — a camada de apresentação não conhece
 * {@code enums}. Foi o {@code ArchitectureTest} que barrou a primeira versão, em que o
 * controller recebia {@code ReportStatus} direto e comparava com {@code OPEN}.
 *
 * <p>Mesmo desenho de {@code ListingSearchRequest}: o VO carrega o critério cru e responde qual
 * é o efetivo.
 */
public record ListingReportSearchRequest(ReportStatus status) {

    /** Sem filtro explícito, a fila mostra o que espera decisão. */
    public ReportStatus effectiveStatus() {
        return status == null ? ReportStatus.OPEN : status;
    }
}
