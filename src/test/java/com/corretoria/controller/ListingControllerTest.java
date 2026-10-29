package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.exception.InvalidListingException;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.ListingSecurity;
import com.corretoria.service.ListingService;
import com.corretoria.vo.v1.response.ListingPhotoResponse;
import com.corretoria.vo.v1.response.ListingResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a <strong>autorização</strong> dos endpoints de anúncio.
 *
 * <p>A Fase 2 inverte a política da Fase 1 em um ponto sensível: parte do catálogo passa a ser
 * pública. Este teste fixa a fronteira exata dessa abertura — ler é livre, escrever exige
 * token, papel e posse —, para que afrouxar um {@code @PreAuthorize} ou ampliar a rota liberada
 * no {@code SecurityConfig} quebre o build em vez de passar despercebido.
 *
 * <p>Mesma montagem do {@code UserControllerTest}: a {@code SecurityConfig} real é importada e
 * só o filtro JWT é substituído por um repasse.
 */
@WebMvcTest(controllers = ListingController.class)
@Import({
        ListingModelAssembler.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        ListingControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class ListingControllerTest {

    private static final long LISTING_ID = 10L;
    private static final long PHOTO_ID = 5L;
    private static final String LISTINGS = "/api/v1/listings";

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
    private ListingService listingService;

    // O nome importa: o @PreAuthorize resolve o bean por "@listingSecurity" no SpEL.
    @MockitoBean(name = "listingSecurity")
    private ListingSecurity listingSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(listingService, listingSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static ListingResponse aResponse() {
        return new ListingResponse(LISTING_ID, ListingType.PROPERTY, OperationType.SALE,
                ListingStatus.ACTIVE, "Apartamento na Aldeota", "Três quartos, nascente",
                new BigDecimal("450000.00"), "Aldeota", "Fortaleza", "CE",
                new BigDecimal("-3.7327"), new BigDecimal("-38.5270"),
                new RealtorSummaryResponse(1L, "João Silva"), null, null, List.of(), null, null);
    }

    private static String creationJson() {
        return """
                {
                  "type": "PROPERTY",
                  "operationType": "SALE",
                  "title": "Apartamento na Aldeota",
                  "description": "Três quartos, nascente",
                  "price": 450000.00,
                  "neighborhood": "Aldeota",
                  "city": "Fortaleza",
                  "state": "CE",
                  "latitude": -3.7327,
                  "longitude": -38.5270,
                  "property": {
                    "propertyType": "APARTMENT",
                    "totalArea": 90.00,
                    "bedrooms": 3,
                    "bathrooms": 2,
                    "garageSpots": 1,
                    "amenities": ["POOL", "ELEVATOR"]
                  }
                }
                """;
    }

    private static String updateJson() {
        return """
                {
                  "operationType": "SALE",
                  "title": "Apartamento reformado",
                  "description": "Três quartos, nascente",
                  "price": 470000.00,
                  "neighborhood": "Aldeota",
                  "city": "Fortaleza",
                  "state": "CE",
                  "latitude": -3.7327,
                  "longitude": -38.5270,
                  "property": {
                    "propertyType": "APARTMENT",
                    "totalArea": 90.00
                  }
                }
                """;
    }

    @Nested
    @DisplayName("catalogo publico")
    class CatalogoPublico {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /listings - catálogo é público - não exige token")
        void shouldAllowAnonymousSearch() throws Exception {
            given(listingService.search(any(), any(), any())).willReturn(Page.empty());

            mockMvc.perform(get(LISTINGS)).andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /listings/{id} - detalhe é público - não exige token")
        void shouldAllowAnonymousDetail() throws Exception {
            given(listingService.findById(LISTING_ID)).willReturn(aResponse());

            mockMvc.perform(get(LISTINGS + "/" + LISTING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(LISTING_ID))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.realtor.href").exists());
        }
    }

    @Nested
    @DisplayName("publicacao")
    class Publicacao {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /listings - sem token - 401 e o serviço nem é chamado")
        void shouldRejectAnonymousPublication() throws Exception {
            mockMvc.perform(post(LISTINGS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isUnauthorized());

            then(listingService).should(never()).publish(any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /listings - cliente - 403: cliente busca, nunca oferta")
        void shouldRejectPublicationFromCustomer() throws Exception {
            mockMvc.perform(post(LISTINGS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isForbidden());

            then(listingService).should(never()).publish(any(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /listings - corretor - 201 com cabeçalho Location")
        void shouldPublishForRealtor() throws Exception {
            given(listingService.publish(any(), any())).willReturn(aResponse());

            mockMvc.perform(post(LISTINGS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isCreated())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().exists("Location"));
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /listings - detalhamento incoerente - 400 com type invalid-listing")
        void shouldTranslateInvalidListingToProblemDetail() throws Exception {
            given(listingService.publish(any(), any()))
                    .willThrow(InvalidListingException.vehicleCannotBeRented());

            mockMvc.perform(post(LISTINGS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.corretoria.app/problems/invalid-listing"));
        }
    }

    @Nested
    @DisplayName("posse do anuncio")
    class PosseDoAnuncio {

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("PUT /listings/{id} - anúncio de outro corretor - 403")
        void shouldRejectUpdateFromNonOwner() throws Exception {
            given(listingSecurity.isOwner(anyLong(), any())).willReturn(false);

            mockMvc.perform(put(LISTINGS + "/" + LISTING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isForbidden());

            then(listingService).should(never()).update(anyLong(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("PUT /listings/{id} - anúncio do próprio corretor - 200")
        void shouldAllowUpdateFromOwner() throws Exception {
            given(listingSecurity.isOwner(anyLong(), any())).willReturn(true);
            given(listingService.update(anyLong(), any())).willReturn(aResponse());

            mockMvc.perform(put(LISTINGS + "/" + LISTING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /listings/{id}/status - administrador - 200 sem ser o dono")
        void shouldAllowStatusChangeFromAdmin() throws Exception {
            given(listingService.changeStatus(anyLong(), any())).willReturn(aResponse());

            mockMvc.perform(patch(LISTINGS + "/" + LISTING_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PAUSED\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("fotos")
    class Fotos {

        private static MockMultipartFile aPhotoPart() {
            return new MockMultipartFile("photo", "fachada.jpg", MediaType.IMAGE_JPEG_VALUE,
                    "conteudo".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /listings/{id}/photos - dono - 201 com a foto criada")
        void shouldUploadPhotoForOwner() throws Exception {
            given(listingSecurity.isOwner(anyLong(), any())).willReturn(true);
            given(listingService.addPhoto(anyLong(), any()))
                    .willReturn(new ListingPhotoResponse(PHOTO_ID,
                            "http://localhost:9000/corretoria-listing-photos/listings/10/f.jpg",
                            1, true));

            mockMvc.perform(multipart(LISTINGS + "/" + LISTING_ID + "/photos")
                            .file(aPhotoPart()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cover").value(true));
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /listings/{id}/photos - anúncio de outro corretor - 403")
        void shouldRejectUploadFromNonOwner() throws Exception {
            given(listingSecurity.isOwner(anyLong(), any())).willReturn(false);

            mockMvc.perform(multipart(LISTINGS + "/" + LISTING_ID + "/photos")
                            .file(aPhotoPart()))
                    .andExpect(status().isForbidden());

            then(listingService).should(never()).addPhoto(anyLong(), any());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("DELETE /listings/{id}/photos/{photoId} - sem token - 401")
        void shouldRejectAnonymousPhotoDeletion() throws Exception {
            mockMvc.perform(delete(LISTINGS + "/" + LISTING_ID + "/photos/" + PHOTO_ID))
                    .andExpect(status().isUnauthorized());

            then(listingService).should(never()).deletePhoto(anyLong(), anyLong());
        }
    }
}
