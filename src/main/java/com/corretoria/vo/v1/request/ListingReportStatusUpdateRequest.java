package com.corretoria.vo.v1.request;

import com.corretoria.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Decisão do moderador sobre uma denúncia.
 *
 * <p>Só {@code REVIEWED} (acolhida) e {@code DISMISSED} (improcedente) são destinos válidos:
 * {@code OPEN} é de onde a denúncia parte, e a coerência é verificada no serviço.
 */
public record ListingReportStatusUpdateRequest(

        @NotNull(message = "A decisão sobre a denúncia é obrigatória")
        ReportStatus status) {
}
