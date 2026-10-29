package com.corretoria.repository;

import com.corretoria.entity.ListingReport;
import com.corretoria.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao agregado raiz {@link ListingReport}. */
public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {

    /** Fila de moderação, filtrada pela situação. */
    Page<ListingReport> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * O denunciante já tem uma denúncia em aberto para este anúncio?
     *
     * <p>Guarda contra enfileirar a mesma reclamação várias vezes. Depois de decidida, uma
     * denúncia nova é legítima: o anúncio pode ter mudado e voltado a ter problema.
     */
    boolean existsByListingIdAndReporterIdAndStatus(
            Long listingId, Long reporterId, ReportStatus status);

    long countByListingId(Long listingId);
}
