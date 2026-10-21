package com.corretoria.mapper;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingReport;
import com.corretoria.entity.User;
import com.corretoria.enums.ReportStatus;
import com.corretoria.vo.v1.request.ListingReportCreationRequest;
import com.corretoria.vo.v1.response.ListingReportResponse;
import com.corretoria.vo.v1.response.ListingSummaryResponse;
import com.corretoria.vo.v1.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Tradução entre {@link ListingReport} e os VOs de contrato.
 *
 * <p>Anúncio, denunciante e situação ficam de fora da entrada: os dois primeiros vêm da rota e
 * do token, e a denúncia nasce sempre em aberto.
 */
@Mapper
public interface ListingReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "status", ignore = true)
    ListingReport toEntity(ListingReportCreationRequest request);

    @Mapping(target = "decidedBy", expression = "java(decidedBy(report))")
    ListingReportResponse toResponse(ListingReport report, long reportCountForListing);

    /**
     * Quem decidiu a denúncia, ou nada enquanto ela está em aberto.
     *
     * <p>A auditoria preenche {@code lastUpdatedBy} já na criação, com o próprio denunciante;
     * expor esse valor como "quem decidiu" antes da decisão apontaria o dedo para a pessoa
     * errada.
     */
    default String decidedBy(ListingReport report) {
        return report.getStatus() == ReportStatus.OPEN ? null : report.getLastUpdatedBy();
    }

    ListingSummaryResponse toListingSummary(Listing listing);

    UserSummaryResponse toUserSummary(User reporter);
}
