package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.enums.NotificationType;
import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.NotificationSecurity;
import com.corretoria.service.NotificationService;
import com.corretoria.vo.v1.response.NotificationResponse;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a <strong>autorização</strong> dos endpoints de notificação.
 *
 * <p>Notificação é correspondência: o teste fixa que nem o administrador marca como lida a
 * notificação alheia — não há escopo administrativo aqui, de propósito.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({
        NotificationModelAssembler.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        NotificationControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class NotificationControllerTest {

    private static final long NOTIFICATION_ID = 42L;
    private static final String NOTIFICATIONS = "/api/v1/notifications";

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
    private NotificationService notificationService;

    // O nome importa: o @PreAuthorize resolve o bean por "@notificationSecurity" no SpEL.
    @MockitoBean(name = "notificationSecurity")
    private NotificationSecurity notificationSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.reset(notificationService, notificationSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static NotificationResponse aResponse() {
        return new NotificationResponse(NOTIFICATION_ID, NotificationType.SEARCH_MATCH,
                "Um anúncio novo casou com a sua busca", true, null);
    }

    @Nested
    @DisplayName("listagem")
    class Listagem {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /notifications - sem token - 401")
        void shouldRejectAnonymousListing() throws Exception {
            mockMvc.perform(get(NOTIFICATIONS)).andExpect(status().isUnauthorized());

            then(notificationService).should(never()).findMine(any(), any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("GET /notifications - autenticado - 200 com o total de não lidas no cabeçalho")
        void shouldListOwnNotifications() throws Exception {
            given(notificationService.findMine(any(), any())).willReturn(Page.empty());
            given(notificationService.countUnread(any())).willReturn(3L);

            mockMvc.perform(get(NOTIFICATIONS))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Corretoria-Unread-Count", "3"));
        }
    }

    @Nested
    @DisplayName("marcar como lida")
    class MarcarComoLida {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PATCH /notifications/{id}/read - destinatário - 200")
        void shouldMarkOwnNotificationAsRead() throws Exception {
            given(notificationSecurity.isRecipient(anyLong(), any())).willReturn(true);
            given(notificationService.markAsRead(NOTIFICATION_ID)).willReturn(aResponse());

            mockMvc.perform(patch(NOTIFICATIONS + "/" + NOTIFICATION_ID + "/read"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("PATCH /notifications/{id}/read - notificação de outro usuário - 403")
        void shouldRejectForeignNotification() throws Exception {
            given(notificationSecurity.isRecipient(anyLong(), any())).willReturn(false);

            mockMvc.perform(patch(NOTIFICATIONS + "/" + NOTIFICATION_ID + "/read"))
                    .andExpect(status().isForbidden());

            then(notificationService).should(never()).markAsRead(anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PATCH /notifications/{id}/read - administrador - 403: correspondência não tem supervisor")
        void shouldRejectAdminOnForeignNotification() throws Exception {
            given(notificationSecurity.isRecipient(anyLong(), any())).willReturn(false);

            mockMvc.perform(patch(NOTIFICATIONS + "/" + NOTIFICATION_ID + "/read"))
                    .andExpect(status().isForbidden());

            then(notificationService).should(never()).markAsRead(anyLong());
        }
    }
}
