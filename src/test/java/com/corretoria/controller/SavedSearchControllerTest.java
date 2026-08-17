package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.enums.ListingType;
import com.corretoria.enums.SearchVisibility;
import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.exception.InvalidSavedSearchException;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.SavedSearchSecurity;
import com.corretoria.service.SavedSearchService;
import com.corretoria.vo.v1.response.CustomerSummaryResponse;
import com.corretoria.vo.v1.response.SavedSearchResponse;
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
 * Verifica a <strong>autorização</strong> dos endpoints de busca salva.
 *
 * <p>Ao contrário do catálogo, aqui nada é público — nem a vitrine de procura. Uma busca
 * revela o que a pessoa quer, onde e por quanto; este teste fixa que nenhuma rota escapa do
 * token e que uma busca só é editada pelo próprio dono.
 */
@WebMvcTest(controllers = SavedSearchController.class)
@Import({
        SavedSearchModelAssembler.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        SavedSearchControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class SavedSearchControllerTest {

    private static final long SEARCH_ID = 7L;
    private static final String SAVED_SEARCHES = "/api/v1/saved-searches";

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
    private SavedSearchService savedSearchService;

    // O nome importa: o @PreAuthorize resolve o bean por "@savedSearchSecurity" no SpEL.
    @MockitoBean(name = "savedSearchSecurity")
    private SavedSearchSecurity savedSearchSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(savedSearchService, savedSearchSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static SavedSearchResponse aResponse() {
        return new SavedSearchResponse(SEARCH_ID, ListingType.PROPERTY, SearchVisibility.PRIVATE,
                "Procuro apartamento na Aldeota", true, null, null, null, "Fortaleza", "CE",
                null, null, null, null, new CustomerSummaryResponse(3L, "Maria Souza"),
                null, null, null, null);
    }

    private static String creationJson() {
        return """
                {
                  "type": "PROPERTY",
                  "visibility": "PRIVATE",
                  "title": "Procuro apartamento na Aldeota",
                  "city": "Fortaleza",
                  "state": "CE",
                  "maxPrice": 500000.00,
                  "propertyCriteria": { "minBedrooms": 2 }
                }
                """;
    }

    private static String updateJson() {
        return """
                {
                  "visibility": "PUBLIC",
                  "title": "Procuro apartamento na Aldeota",
                  "active": true,
                  "city": "Fortaleza",
                  "state": "CE",
                  "propertyCriteria": { "minBedrooms": 3 }
                }
                """;
    }

    @Nested
    @DisplayName("criacao")
    class Criacao {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /saved-searches - sem token - 401")
        void shouldRejectAnonymousCreation() throws Exception {
            mockMvc.perform(post(SAVED_SEARCHES)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isUnauthorized());

            then(savedSearchService).should(never()).create(any(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /saved-searches - corretor - 403: quem oferta não procura")
        void shouldRejectCreationFromRealtor() throws Exception {
            mockMvc.perform(post(SAVED_SEARCHES)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isForbidden());

            then(savedSearchService).should(never()).create(any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /saved-searches - cliente - 201 com cabeçalho Location")
        void shouldCreateForCustomer() throws Exception {
            given(savedSearchService.create(any(), any())).willReturn(aResponse());

            mockMvc.perform(post(SAVED_SEARCHES)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isCreated())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().exists("Location"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /saved-searches - critérios de outro tipo - 400 invalid-saved-search")
        void shouldTranslateInvalidSavedSearchToProblemDetail() throws Exception {
            given(savedSearchService.create(any(), any()))
                    .willThrow(InvalidSavedSearchException.criteriaDoNotMatchType());

            mockMvc.perform(post(SAVED_SEARCHES)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.type")
                            .value("https://api.corretoria.app/problems/invalid-saved-search"));
        }
    }

    @Nested
    @DisplayName("vitrine de procura")
    class VitrineDeProcura {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /saved-searches/public - sem token - 401: procura não é vitrine aberta")
        void shouldRejectAnonymousShowcase() throws Exception {
            mockMvc.perform(get(SAVED_SEARCHES + "/public"))
                    .andExpect(status().isUnauthorized());

            then(savedSearchService).should(never()).findPublic(any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("GET /saved-searches/public - corretor autenticado - 200")
        void shouldAllowShowcaseForRealtor() throws Exception {
            given(savedSearchService.findPublic(any())).willReturn(Page.empty());

            mockMvc.perform(get(SAVED_SEARCHES + "/public")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("GET /saved-searches - corretor - 403: a coleção é das buscas do cliente")
        void shouldRejectOwnListingForRealtor() throws Exception {
            mockMvc.perform(get(SAVED_SEARCHES)).andExpect(status().isForbidden());

            then(savedSearchService).should(never()).findMine(any(), any());
        }
    }

    @Nested
    @DisplayName("posse da busca")
    class PosseDaBusca {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PUT /saved-searches/{id} - busca de outro cliente - 403")
        void shouldRejectUpdateFromNonOwner() throws Exception {
            given(savedSearchSecurity.isOwner(anyLong(), any())).willReturn(false);

            mockMvc.perform(put(SAVED_SEARCHES + "/" + SEARCH_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isForbidden());

            then(savedSearchService).should(never()).update(anyLong(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PUT /saved-searches/{id} - busca do próprio cliente - 200")
        void shouldAllowUpdateFromOwner() throws Exception {
            given(savedSearchSecurity.isOwner(anyLong(), any())).willReturn(true);
            given(savedSearchService.update(anyLong(), any())).willReturn(aResponse());

            mockMvc.perform(put(SAVED_SEARCHES + "/" + SEARCH_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("DELETE /saved-searches/{id} - busca de outro cliente - 403")
        void shouldRejectDeletionFromNonOwner() throws Exception {
            given(savedSearchSecurity.isOwner(anyLong(), any())).willReturn(false);

            mockMvc.perform(delete(SAVED_SEARCHES + "/" + SEARCH_ID))
                    .andExpect(status().isForbidden());

            then(savedSearchService).should(never()).delete(anyLong());
        }
    }
}
