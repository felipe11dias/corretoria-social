package com.corretoria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corretoria.exception.GlobalExceptionHandler;
import com.corretoria.security.JwtAuthenticationEntryPoint;
import com.corretoria.security.JwtAuthenticationFilter;
import com.corretoria.security.UserSecurity;
import com.corretoria.service.UserService;
import com.corretoria.vo.v1.response.UserResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a <strong>autorização</strong> dos endpoints de usuário.
 *
 * <p>Até aqui, posse e escopo administrativo só haviam sido conferidos à mão, com a aplicação
 * no ar. Sem este teste, afrouxar um {@code @PreAuthorize} passaria pelo build sem ninguém
 * perceber — e uma falha de autorização é exatamente o tipo de regressão que não pode
 * depender de conferência manual.
 *
 * <p>A {@code SecurityConfig} real é importada, de modo que as regras de rota pública e
 * protegida também sejam exercitadas. Só o filtro JWT é substituído por um repasse: o token
 * em si já é coberto por {@code JwtServiceTest}.
 */
@WebMvcTest(controllers = UserController.class)
@Import({
        UserModelAssembler.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        com.corretoria.config.SecurityConfig.class,
        SpringDataWebAutoConfiguration.class,
        UserControllerTest.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "corretoria.problem.base-uri=https://api.corretoria.app",
        "corretoria.jwt.secret=segredo-de-teste-com-mais-de-256-bits-de-tamanho",
        "corretoria.jwt.expiration=3600000"
})
class UserControllerTest {

    private static final long OWN_ID = 1L;
    private static final long OTHER_ID = 2L;
    private static final String USERS = "/api/v1/users";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-25T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // O nome importa: o @PreAuthorize resolve o bean por "@userSecurity" no SpEL, e sem
    // nomeá-lo o mock entra no contexto com nome gerado.
    @MockitoBean(name = "userSecurity")
    private UserSecurity userSecurity;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        // Os beans mockados são compartilhados pelos testes do mesmo contexto; sem reiniciar,
        // uma verificação de "nunca chamado" enxergaria a chamada do teste anterior.
        org.mockito.Mockito.reset(userService, userSecurity);

        willAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0, ServletRequest.class),
                    call.getArgument(1, ServletResponse.class));
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static UserResponse aResponse(long id) {
        return new UserResponse(id, "João Silva", "joao@email.com", "joao.silva",
                Set.of(), List.of(), null, null);
    }

    private static String registrationJson() {
        return """
                {
                  "name": "João Silva",
                  "email": "joao.silva@email.com",
                  "login": "joao.silva",
                  "password": "senhaSegura123",
                  "roles": ["ROLE_REALTOR"],
                  "addresses": []
                }
                """;
    }

    private static String updateJson() {
        return """
                {
                  "name": "João Silva",
                  "addresses": []
                }
                """;
    }

    private static String passwordJson() {
        return """
                {
                  "currentPassword": "senhaSegura123",
                  "newPassword": "novaSenha456",
                  "confirmPassword": "novaSenha456"
                }
                """;
    }

    @Nested
    @DisplayName("acesso sem autenticacao")
    class SemAutenticacao {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /users - autocadastro é público - não exige token")
        void shouldAllowAnonymousRegistration() throws Exception {
            given(userService.register(any())).willReturn(aResponse(OWN_ID));

            mockMvc.perform(post(USERS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson()))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /users/{id} - sem token - 401 com missing-token")
        void shouldRejectAnonymousRead() throws Exception {
            mockMvc.perform(get(USERS + "/" + OWN_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.corretoria.app/problems/missing-token"));

            then(userService).shouldHaveNoInteractions();
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /users - listagem sem token - 401")
        void shouldRejectAnonymousList() throws Exception {
            mockMvc.perform(get(USERS)).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("autorizacao por posse")
    class PorPosse {

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("GET /users/{id} - próprio cadastro - 200")
        void shouldAllowOwnerToRead() throws Exception {
            given(userSecurity.isSelf(eq(OWN_ID), any())).willReturn(true);
            given(userService.findById(OWN_ID)).willReturn(aResponse(OWN_ID));

            mockMvc.perform(get(USERS + "/" + OWN_ID)).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("GET /users/{id} - cadastro de outro - 403 e o serviço nem é chamado")
        void shouldDenyForeignRead() throws Exception {
            given(userSecurity.isSelf(eq(OTHER_ID), any())).willReturn(false);

            mockMvc.perform(get(USERS + "/" + OTHER_ID)).andExpect(status().isForbidden());

            then(userService).should(never()).findById(anyLong());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("PUT /users/{id} - cadastro de outro - 403")
        void shouldDenyForeignUpdate() throws Exception {
            given(userSecurity.isSelf(eq(OTHER_ID), any())).willReturn(false);

            mockMvc.perform(put(USERS + "/" + OTHER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson()))
                    .andExpect(status().isForbidden());

            then(userService).should(never()).update(anyLong(), any());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("PATCH /users/{id}/password - senha de outro - 403")
        void shouldDenyForeignPasswordChange() throws Exception {
            given(userSecurity.isSelf(eq(OTHER_ID), any())).willReturn(false);

            mockMvc.perform(patch(USERS + "/" + OTHER_ID + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(passwordJson()))
                    .andExpect(status().isForbidden());

            then(userService).should(never()).changePassword(anyLong(), any());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("DELETE /users/{id} - cadastro de outro - 403")
        void shouldDenyForeignDelete() throws Exception {
            given(userSecurity.isSelf(eq(OTHER_ID), any())).willReturn(false);

            mockMvc.perform(delete(USERS + "/" + OTHER_ID)).andExpect(status().isForbidden());

            then(userService).should(never()).delete(anyLong());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("PATCH /users/{id}/password - própria senha - 204")
        void shouldAllowOwnPasswordChange() throws Exception {
            given(userSecurity.isSelf(eq(OWN_ID), any())).willReturn(true);

            mockMvc.perform(patch(USERS + "/" + OWN_ID + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(passwordJson()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("DELETE /users/{id} - próprio cadastro - 204")
        void shouldAllowOwnDelete() throws Exception {
            given(userSecurity.isSelf(eq(OWN_ID), any())).willReturn(true);

            mockMvc.perform(delete(USERS + "/" + OWN_ID)).andExpect(status().isNoContent());

            then(userService).should().delete(OWN_ID);
        }
    }

    @Nested
    @DisplayName("escopo administrativo")
    class EscopoAdministrativo {

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("GET /users - listagem por usuário comum - 403")
        void shouldDenyListToCommonUser() throws Exception {
            mockMvc.perform(get(USERS)).andExpect(status().isForbidden());

            then(userService).should(never()).findAll(any());
        }

        @Test
        @WithMockUser(username = "joao.silva", roles = "REALTOR")
        @DisplayName("GET /users?name=... - busca por usuário comum - 403")
        void shouldDenySearchToCommonUser() throws Exception {
            mockMvc.perform(get(USERS).param("name", "jo"))
                    .andExpect(status().isForbidden());

            then(userService).should(never()).findByName(any(), any());
        }

        @Test
        @WithMockUser(username = "admin.demo", roles = "ADMIN")
        @DisplayName("GET /users/{id} - admin acessa qualquer cadastro sem ser o dono - 200")
        void shouldAllowAdminToReadAnyUser() throws Exception {
            given(userSecurity.isSelf(eq(OTHER_ID), any())).willReturn(false);
            given(userService.findById(OTHER_ID)).willReturn(aResponse(OTHER_ID));

            mockMvc.perform(get(USERS + "/" + OTHER_ID)).andExpect(status().isOk());
        }
    }
}
