package com.corretoria.vo.v1.request;

import com.corretoria.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Abertura de uma denúncia.
 *
 * <p>O anúncio vem da rota e o denunciante vem do token — nenhum dos dois no corpo. Denunciar
 * em nome de outra pessoa seria uma forma barata de perseguir um corretor.
 *
 * @param reason classificação obrigatória, de lista fechada: é ela que permite priorizar a
 *               fila, porque suspeita de fraude não espera na mesma ordem que uma foto
 *               desatualizada
 * @param detail complemento em texto livre, opcional
 */
public record ListingReportCreationRequest(

        @NotNull(message = "O motivo da denúncia é obrigatório")
        ReportReason reason,

        @Size(max = 2000, message = "O detalhamento deve ter no máximo 2000 caracteres")
        String detail) {
}
