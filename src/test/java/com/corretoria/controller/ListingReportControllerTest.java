package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.enums.ReportReason;
import com.corretoria.enums.ReportStatus;
import com.corretoria.exception.DuplicateResourceException;
import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.service.ListingReportService;
import com.corretoria.vo.v1.response.ListingReportResponse;
import com.corretoria.vo.v1.response.UserSummaryResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a <strong>autorização</strong> da moderação.
 *
 * <p>A assimetria do cluster é o que este teste fixa: abrir denúncia é de qualquer usuário
 * autenticado — o contrapeso da publicação imediata só funciona se denunciar for fácil —,
 * enquanto ver a fila e decidir é exclusivo do administrador.
 */
@WebMvcTest(controllers = ListingReportController.class)
@Import({
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        ListingReportControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class ListingReportControllerTest {

    private static final long LISTING_ID = 10L;
    private static final long REPORT_ID = 70L;
    private static final String API = "/api/v1";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingReportService listingReportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(listingReportService);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static ListingReportResponse aResponse(ReportStatus status) {
        return new ListingReportResponse(REPORT_ID, null,
                new UserSummaryResponse(2L, "Cliente Demonstracao"),
                ReportReason.SUSPECTED_FRAUD, "Preço fora do mercado", status,
                2, status == ReportStatus.OPEN ? null : "admin.demo", null, null);
    }

    private static String creationJson() {
        return """
                {
                  "reason": "SUSPECTED_FRAUD",
                  "detail": "O preço está muito abaixo do mercado."
                }
                """;
    }

    @Nested
    @DisplayName("abrir denuncia")
    class AbrirDenuncia {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /listings/{id}/reports - sem token - 401")
        void shouldRejectAnonymousReport() throws Exception {
            mockMvc.perform(post(API + "/listings/" + LISTING_ID + "/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isUnauthorized());

            then(listingReportService).should(never()).report(anyLong(), any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /listings/{id}/reports - cliente - 201: denunciar é de qualquer autenticado")
        void shouldLetCustomerReport() throws Exception {
            given(listingReportService.report(anyLong(), any(), any()))
                    .willReturn(aResponse(ReportStatus.OPEN));

            mockMvc.perform(post(API + "/listings/" + LISTING_ID + "/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.decidedBy").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /listings/{id}/reports - corretor - 201: também denuncia")
        void shouldLetRealtorReport() throws Exception {
            given(listingReportService.report(anyLong(), any(), any()))
                    .willReturn(aResponse(ReportStatus.OPEN));

            mockMvc.perform(post(API + "/listings/" + LISTING_ID + "/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /listings/{id}/reports - segunda denúncia em aberto - 409")
        void shouldTranslateDuplicateReportToConflict() throws Exception {
            given(listingReportService.report(anyLong(), any(), any()))
                    .willThrow(DuplicateResourceException.openListingReport(LISTING_ID));

            mockMvc.perform(post(API + "/listings/" + LISTING_ID + "/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.corretoria.app/problems/duplicate-resource"));
        }
    }

    @Nested
    @DisplayName("fila de moderacao")
    class FilaDeModeracao {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /reports - cliente - 403: a fila é ferramenta do moderador")
        void shouldRejectQueueForCustomer() throws Exception {
            mockMvc.perform(get(API + "/reports")).andExpect(status().isForbidden());

            then(listingReportService).should(never()).findQueue(any(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("GET /reports - corretor - 403: nem o dono do anúncio denunciado entra")
        void shouldRejectQueueForRealtor() throws Exception {
            mockMvc.perform(get(API + "/reports")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /reports - administrador - 200")
        void shouldAllowQueueForAdmin() throws Exception {
            given(listingReportService.findQueue(any(), any())).willReturn(Page.empty());

            mockMvc.perform(get(API + "/reports")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("decisao")
    class Decisao {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PATCH /reports/{id}/status - cliente - 403: decisão não é opinião de usuário")
        void shouldRejectDecisionFromCustomer() throws Exception {
            mockMvc.perform(patch(API + "/reports/" + REPORT_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DISMISSED\"}"))
                    .andExpect(status().isForbidden());

            then(listingReportService).should(never()).decide(anyLong(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /reports/{id}/status - administrador - 200 com quem decidiu")
        void shouldLetAdminDecide() throws Exception {
            given(listingReportService.decide(anyLong(), any()))
                    .willReturn(aResponse(ReportStatus.REVIEWED));

            mockMvc.perform(patch(API + "/reports/" + REPORT_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"REVIEWED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REVIEWED"))
                    .andExpect(jsonPath("$.decidedBy").value("admin.demo"));
        }
    }
}
