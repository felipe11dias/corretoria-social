package com.corretoria.service;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingReport;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.ReportStatus;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.DuplicateResourceException;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.ListingReportMapper;
import com.corretoria.repository.ListingReportRepository;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.ListingReportCreationRequest;
import com.corretoria.vo.v1.request.ListingReportSearchRequest;
import com.corretoria.vo.v1.request.ListingReportStatusUpdateRequest;
import com.corretoria.vo.v1.response.ListingReportResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio da moderação.
 *
 * <p>É o contrapeso da publicação imediata: qualquer usuário autenticado abre uma denúncia, e
 * só o administrador a decide. A assimetria é deliberada — abrir precisa ser fácil, decidir
 * precisa ser restrito.
 *
 * <p>Nada aqui mexe no anúncio denunciado. Acolher a denúncia registra a decisão; retirar o
 * anúncio do ar continua sendo uma ação explícita do administrador sobre o anúncio, pelo
 * endpoint de situação. Separar as duas evita que um clique na fila apague o anúncio de alguém
 * por engano.
 */
@Service
public class ListingReportService {

    private final ListingReportRepository listingReportRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingReportMapper listingReportMapper;
    private final NotificationService notificationService;
    private final UserSecurity userSecurity;

    public ListingReportService(
            ListingReportRepository listingReportRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            ListingReportMapper listingReportMapper,
            NotificationService notificationService,
            UserSecurity userSecurity) {
        this.listingReportRepository = listingReportRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.listingReportMapper = listingReportMapper;
        this.notificationService = notificationService;
        this.userSecurity = userSecurity;
    }

    /**
     * Abre uma denúncia sobre um anúncio.
     *
     * <p>Avisa os moderadores, e não o corretor denunciado: contar a ele antes da análise daria
     * a quem agiu de má-fé o tempo de apagar o rastro.
     */
    @Transactional
    public ListingReportResponse report(
            Long listingId, ListingReportCreationRequest request, Authentication authentication) {

        User reporter = userSecurity.getAuthenticatedUser(authentication);
        Listing listing = getListingOrThrow(listingId);

        ensureReporterIsNotTheOwner(reporter, listing);
        ensureThereIsNoOpenReport(listingId, reporter);

        ListingReport report = listingReportMapper.toEntity(request);
        report.setListing(listing);
        report.setReporter(reporter);

        ListingReport created = listingReportRepository.save(report);
        notifyModerators(created);

        return toResponse(created);
    }

    /** Fila de moderação, da denúncia mais antiga para a mais nova. */
    @Transactional(readOnly = true)
    public Page<ListingReportResponse> findQueue(
            ListingReportSearchRequest request, Pageable pageable) {

        return listingReportRepository.findByStatus(request.effectiveStatus(), pageable)
                .map(this::toResponse);
    }

    /**
     * Decide a denúncia.
     *
     * <p>Decisão é definitiva, como o desfecho de uma proposta: a mesma denúncia não volta para
     * a fila. Um problema que reaparece é uma denúncia nova, com data nova.
     */
    @Transactional
    public ListingReportResponse decide(Long id, ListingReportStatusUpdateRequest request) {
        ListingReport report = listingReportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.listingReport(id));

        ensureReportIsOpen(report);
        ensureDecisionIsFinal(request.status());

        report.setStatus(request.status());
        return toResponse(listingReportRepository.save(report));
    }

    /**
     * O anúncio denunciado acumula quantas denúncias?
     *
     * <p>Vai junto da resposta porque é a pergunta seguinte de quem modera: uma reclamação
     * isolada e um padrão pedem reações diferentes.
     */
    private ListingReportResponse toResponse(ListingReport report) {
        return listingReportMapper.toResponse(
                report, listingReportRepository.countByListingId(report.getListing().getId()));
    }

    private void notifyModerators(ListingReport report) {
        List<User> moderators = userRepository.findByRolesName(RoleName.ROLE_ADMIN);
        String message = buildMessage(report);

        for (User moderator : moderators) {
            notificationService.notify(moderator, NotificationType.LISTING_REPORTED, message);
        }
    }

    private String buildMessage(ListingReport report) {
        return """
                Denúncia de %s sobre o anúncio "%s".
                Veja a fila em /api/v1/reports."""
                .formatted(report.getReason(), report.getListing().getTitle());
    }

    private void ensureReporterIsNotTheOwner(User reporter, Listing listing) {
        if (listing.getRealtor().getId().equals(reporter.getId())) {
            throw ForbiddenOperationException.cannotReportOwnListing();
        }
    }

    private void ensureThereIsNoOpenReport(Long listingId, User reporter) {
        boolean alreadyReported = listingReportRepository
                .existsByListingIdAndReporterIdAndStatus(
                        listingId, reporter.getId(), ReportStatus.OPEN);

        if (alreadyReported) {
            throw DuplicateResourceException.openListingReport(listingId);
        }
    }

    private void ensureReportIsOpen(ListingReport report) {
        if (report.getStatus() != ReportStatus.OPEN) {
            throw ForbiddenOperationException.reportAlreadyDecided();
        }
    }

    private void ensureDecisionIsFinal(ReportStatus status) {
        if (status == ReportStatus.OPEN) {
            throw ForbiddenOperationException.reportDecisionMustBeFinal();
        }
    }

    private Listing getListingOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.listing(id));
    }
}
