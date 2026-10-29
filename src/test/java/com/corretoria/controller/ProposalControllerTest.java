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

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.ProposalInitiator;
import com.corretoria.enums.ProposalStatus;
import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.exception.InvalidProposalException;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.ProposalSecurity;
import com.corretoria.service.ProposalService;
import com.corretoria.vo.v1.response.CustomerSummaryResponse;
import com.corretoria.vo.v1.response.ListingSummaryResponse;
import com.corretoria.vo.v1.response.ProposalResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
 * Verifica a <strong>autorização</strong> dos endpoints de negociação.
 *
 * <p>O thread de uma proposta é conversa particular entre dois participantes. Este teste fixa
 * que ninguém de fora entra — nem o administrador —, e que o link de desfecho some quando a
 * proposta já foi decidida.
 */
@WebMvcTest(controllers = ProposalController.class)
@Import({
        ProposalModelAssembler.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        ProposalControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class ProposalControllerTest {

    private static final long PROPOSAL_ID = 30L;
    private static final String PROPOSALS = "/api/v1/proposals";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-16T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalService proposalService;

    // O nome importa: o @PreAuthorize resolve o bean por "@proposalSecurity" no SpEL.
    @MockitoBean(name = "proposalSecurity")
    private ProposalSecurity proposalSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(proposalService, proposalSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static ProposalResponse aResponse(ProposalStatus status) {
        return new ProposalResponse(PROPOSAL_ID,
                new ListingSummaryResponse(10L, ListingType.PROPERTY, ListingStatus.ACTIVE,
                        "Apartamento na Aldeota", new BigDecimal("450000.00")),
                new CustomerSummaryResponse(2L, "Maria Souza"),
                new RealtorSummaryResponse(1L, "João Silva"),
                ProposalInitiator.CUSTOMER, status, new BigDecimal("430000.00"),
                null, List.of(), null, null);
    }

    private static String creationJson() {
        return """
                {
                  "listingId": 10,
                  "amount": 430000.00
                }
                """;
    }

    @Nested
    @DisplayName("abertura")
    class Abertura {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /proposals - sem token - 401")
        void shouldRejectAnonymousCreation() throws Exception {
            mockMvc.perform(post(PROPOSALS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isUnauthorized());

            then(proposalService).should(never()).create(any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /proposals - autenticado - 201 com cabeçalho Location")
        void shouldCreateForAuthenticatedUser() throws Exception {
            given(proposalService.create(any(), any())).willReturn(aResponse(ProposalStatus.PENDING));

            mockMvc.perform(post(PROPOSALS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isCreated())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().exists("Location"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /proposals - anúncio fora do ar - 400 invalid-proposal")
        void shouldTranslateInvalidProposalToProblemDetail() throws Exception {
            given(proposalService.create(any(), any()))
                    .willThrow(InvalidProposalException.listingIsNotActive());

            mockMvc.perform(post(PROPOSALS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.corretoria.app/problems/invalid-proposal"));
        }
    }

    @Nested
    @DisplayName("participacao")
    class Participacao {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /proposals/{id} - negociação de terceiros - 403")
        void shouldRejectNonParticipant() throws Exception {
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(false);

            mockMvc.perform(get(PROPOSALS + "/" + PROPOSAL_ID))
                    .andExpect(status().isForbidden());

            then(proposalService).should(never()).findById(anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /proposals/{id} - administrador - 403: negociação não tem plateia")
        void shouldRejectAdmin() throws Exception {
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(false);

            mockMvc.perform(get(PROPOSALS + "/" + PROPOSAL_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /proposals/{id} - participante - 200 com link de desfecho enquanto pendente")
        void shouldExposeStatusLinkWhilePending() throws Exception {
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(true);
            given(proposalService.findById(PROPOSAL_ID))
                    .willReturn(aResponse(ProposalStatus.PENDING));

            mockMvc.perform(get(PROPOSALS + "/" + PROPOSAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.status.href").exists())
                    .andExpect(jsonPath("$._links.messages.href").exists());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /proposals/{id} - proposta decidida - sem link de desfecho")
        void shouldHideStatusLinkWhenDecided() throws Exception {
            // Oferecer o caminho depois do desfecho seria convidar o cliente da API a um 400.
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(true);
            given(proposalService.findById(PROPOSAL_ID))
                    .willReturn(aResponse(ProposalStatus.ACCEPTED));

            mockMvc.perform(get(PROPOSALS + "/" + PROPOSAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.status").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("PATCH /proposals/{id}/status - não participante - 403")
        void shouldRejectStatusChangeFromNonParticipant() throws Exception {
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(false);

            mockMvc.perform(patch(PROPOSALS + "/" + PROPOSAL_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isForbidden());

            then(proposalService).should(never()).changeStatus(anyLong(), any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /proposals/{id}/messages - não participante - 403")
        void shouldRejectMessageFromNonParticipant() throws Exception {
            given(proposalSecurity.isParticipant(anyLong(), any())).willReturn(false);

            mockMvc.perform(post(PROPOSALS + "/" + PROPOSAL_ID + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Aceito por 440 mil?\"}"))
                    .andExpect(status().isForbidden());

            then(proposalService).should(never()).addMessage(anyLong(), any(), any());
        }
    }

    @Nested
    @DisplayName("listagem")
    class Listagem {

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("GET /proposals - autenticado - 200: uma lista com os dois papéis")
        void shouldListOwnProposals() throws Exception {
            given(proposalService.findMine(any(), any())).willReturn(Page.empty());

            mockMvc.perform(get(PROPOSALS)).andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /proposals - sem token - 401")
        void shouldRejectAnonymousListing() throws Exception {
            mockMvc.perform(get(PROPOSALS)).andExpect(status().isUnauthorized());
        }
    }
}
