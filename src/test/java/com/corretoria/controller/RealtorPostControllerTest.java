package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.RealtorPostSecurity;
import com.corretoria.service.RealtorPostService;
import com.corretoria.vo.v1.response.RealtorPostResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
 * Verifica a <strong>autorização</strong> do mural.
 *
 * <p>Três fronteiras distintas convivem aqui, e o teste fixa as três: ler é público, publicar é
 * do dono do mural, e comentar ou curtir é de qualquer usuário autenticado — inclusive de
 * outro corretor.
 */
@WebMvcTest(controllers = RealtorPostController.class)
@Import({
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        RealtorPostControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class RealtorPostControllerTest {

    private static final long REALTOR_ID = 1L;
    private static final long POST_ID = 50L;
    private static final String API = "/api/v1";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-26T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RealtorPostService realtorPostService;

    // O nome importa: o @PreAuthorize resolve o bean por "@realtorPostSecurity" no SpEL.
    @MockitoBean(name = "realtorPostSecurity")
    private RealtorPostSecurity realtorPostSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(realtorPostService, realtorPostSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static RealtorPostResponse aResponse() {
        return new RealtorPostResponse(POST_ID,
                new RealtorSummaryResponse(REALTOR_ID, "Joao Silva"),
                "O mercado da Aldeota em agosto", 3, List.of(), null, null);
    }

    @Nested
    @DisplayName("publicacao")
    class Publicacao {

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /realtors/{id}/posts - mural de outro corretor - 403")
        void shouldRejectPublishingOnForeignWall() throws Exception {
            // Publicar no mural de alguém é falar em nome dele.
            given(realtorPostSecurity.isOwnWall(anyLong(), any())).willReturn(false);

            mockMvc.perform(post(API + "/realtors/" + REALTOR_ID + "/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Texto\"}"))
                    .andExpect(status().isForbidden());

            then(realtorPostService).should(never()).publish(any(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("POST /realtors/{id}/posts - próprio mural - 201")
        void shouldPublishOnOwnWall() throws Exception {
            given(realtorPostSecurity.isOwnWall(anyLong(), any())).willReturn(true);
            given(realtorPostService.publish(any(), any())).willReturn(aResponse());

            mockMvc.perform(post(API + "/realtors/" + REALTOR_ID + "/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"O mercado da Aldeota em agosto\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /realtors/{id}/posts - mural é público - 200 sem token")
        void shouldAllowAnonymousReading() throws Exception {
            given(realtorPostService.findByRealtor(anyLong(), any())).willReturn(Page.empty());

            mockMvc.perform(get(API + "/realtors/" + REALTOR_ID + "/posts"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("interacao")
    class Interacao {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("POST /posts/{id}/comments - cliente autenticado - 201")
        void shouldLetCustomerComment() throws Exception {
            mockMvc.perform(post(API + "/posts/" + POST_ID + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Otima analise!\"}"))
                    .andExpect(status().isCreated());

            then(realtorPostService).should().comment(anyLong(), any(), any());
        }

        @Test
        @WithMockUser(roles = "REALTOR")
        @DisplayName("PUT /posts/{id}/like - outro corretor - 200: mural é conversa aberta")
        void shouldLetAnotherRealtorLike() throws Exception {
            given(realtorPostService.like(anyLong(), any())).willReturn(aResponse());

            mockMvc.perform(put(API + "/posts/" + POST_ID + "/like"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("PUT /posts/{id}/like - sem token - 401")
        void shouldRejectAnonymousLike() throws Exception {
            mockMvc.perform(put(API + "/posts/" + POST_ID + "/like"))
                    .andExpect(status().isUnauthorized());

            then(realtorPostService).should(never()).like(anyLong(), any());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /posts/{id}/comments - sem token - 401")
        void shouldRejectAnonymousComment() throws Exception {
            mockMvc.perform(post(API + "/posts/" + POST_ID + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Texto\"}"))
                    .andExpect(status().isUnauthorized());

            then(realtorPostService).should(never()).comment(anyLong(), any(), any());
        }
    }
}
