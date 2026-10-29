package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingReport;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.ReportReason;
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
import com.corretoria.vo.v1.request.ListingReportStatusUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class ListingReportServiceTest {

    private static final Long LISTING_ID = 10L;
    private static final Long REPORT_ID = 70L;
    private static final Long REALTOR_ID = 1L;
    private static final Long REPORTER_ID = 2L;

    @Mock
    private ListingReportRepository listingReportRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingReportMapper listingReportMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private ListingReportService listingReportService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "cliente.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario " + id);
        return user;
    }

    private static Listing aListing() {
        Listing listing = new Listing();
        listing.setId(LISTING_ID);
        listing.setTitle("Apartamento na Aldeota");
        listing.setRealtor(aUser(REALTOR_ID));
        return listing;
    }

    private static ListingReport aReport(ReportStatus status) {
        ListingReport report = new ListingReport();
        report.setId(REPORT_ID);
        report.setListing(aListing());
        report.setReporter(aUser(REPORTER_ID));
        report.setReason(ReportReason.SUSPECTED_FRAUD);
        report.setStatus(status);
        return report;
    }

    private static ListingReportCreationRequest aRequest() {
        return new ListingReportCreationRequest(
                ReportReason.SUSPECTED_FRAUD, "O preço está muito abaixo do mercado.");
    }

    @Nested
    @DisplayName("report")
    class Report {

        @Test
        @DisplayName("report - usuário autenticado - registra em aberto e avisa os moderadores")
        void shouldOpenReportAndNotifyModerators() {
            // O avisado é quem pode agir: contar ao corretor denunciado antes da análise daria
            // a quem agiu de má-fé o tempo de apagar o rastro.
            Authentication authentication = anAuthentication();
            ListingReport report = aReport(ReportStatus.OPEN);

            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REPORTER_ID));
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(aListing()));
            given(listingReportRepository.existsByListingIdAndReporterIdAndStatus(
                    LISTING_ID, REPORTER_ID, ReportStatus.OPEN)).willReturn(false);
            given(listingReportMapper.toEntity(any(ListingReportCreationRequest.class)))
                    .willReturn(report);
            given(listingReportRepository.save(report)).willReturn(report);
            given(userRepository.findByRolesName(RoleName.ROLE_ADMIN))
                    .willReturn(List.of(aUser(99L)));

            listingReportService.report(LISTING_ID, aRequest(), authentication);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
            then(notificationService).should()
                    .notify(any(), eq(NotificationType.LISTING_REPORTED), any());
        }

        @Test
        @DisplayName("report - denúncia do próprio anúncio - lança ForbiddenOperationException")
        void shouldRejectReportFromOwner() {
            // Para o que é dele, o corretor já tem o endpoint de situação.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID));
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(aListing()));

            assertThatThrownBy(() ->
                    listingReportService.report(LISTING_ID, aRequest(), authentication))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("próprio anúncio");

            then(listingReportRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("report - segunda denúncia em aberto do mesmo usuário - lança DuplicateResourceException")
        void shouldRejectSecondOpenReport() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REPORTER_ID));
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(aListing()));
            given(listingReportRepository.existsByListingIdAndReporterIdAndStatus(
                    LISTING_ID, REPORTER_ID, ReportStatus.OPEN)).willReturn(true);

            assertThatThrownBy(() ->
                    listingReportService.report(LISTING_ID, aRequest(), authentication))
                    .isInstanceOf(DuplicateResourceException.class);

            then(listingReportRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("report - anúncio inexistente - lança ResourceNotFoundException")
        void shouldRejectUnknownListing() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REPORTER_ID));
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    listingReportService.report(LISTING_ID, aRequest(), authentication))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("decide")
    class Decide {

        @Test
        @DisplayName("decide - denúncia em aberto - acolhe sem mexer no anúncio")
        void shouldReviewOpenReport() {
            // Retirar o anúncio do ar continua sendo ação explícita sobre ele: um clique na
            // fila não pode apagar o anúncio de alguém por engano.
            ListingReport report = aReport(ReportStatus.OPEN);
            given(listingReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));
            given(listingReportRepository.save(report)).willReturn(report);

            listingReportService.decide(REPORT_ID,
                    new ListingReportStatusUpdateRequest(ReportStatus.REVIEWED));

            assertThat(report.getStatus()).isEqualTo(ReportStatus.REVIEWED);
            then(listingRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("decide - denúncia já decidida - lança ForbiddenOperationException")
        void shouldRejectDecidingTwice() {
            ListingReport report = aReport(ReportStatus.DISMISSED);
            given(listingReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));

            assertThatThrownBy(() -> listingReportService.decide(REPORT_ID,
                    new ListingReportStatusUpdateRequest(ReportStatus.REVIEWED)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("já foi decidida");
        }

        @Test
        @DisplayName("decide - destino OPEN - lança ForbiddenOperationException")
        void shouldRejectReturningToQueue() {
            ListingReport report = aReport(ReportStatus.OPEN);
            given(listingReportRepository.findById(REPORT_ID)).willReturn(Optional.of(report));

            assertThatThrownBy(() -> listingReportService.decide(REPORT_ID,
                    new ListingReportStatusUpdateRequest(ReportStatus.OPEN)))
                    .isInstanceOf(ForbiddenOperationException.class);

            then(listingReportRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("decide - denúncia inexistente - lança ResourceNotFoundException")
        void shouldRejectUnknownReport() {
            given(listingReportRepository.findById(REPORT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> listingReportService.decide(REPORT_ID,
                    new ListingReportStatusUpdateRequest(ReportStatus.REVIEWED)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Denúncia não encontrada");
        }
    }
}
