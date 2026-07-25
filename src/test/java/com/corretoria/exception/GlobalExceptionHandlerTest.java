package com.corretoria.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private static final String BASE_URI = "https://api.corretoria.app";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-25T10:00:00Z");
    private static final String PATH = "/api/v1/users";

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(BASE_URI, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

    private final MockHttpServletRequest request = requestTo(PATH);

    private static MockHttpServletRequest requestTo(String path) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", path);
        mockRequest.setRequestURI(path);
        return mockRequest;
    }

    @Nested
    @DisplayName("mapeamento de status e type")
    class MapeamentoDeStatusEType {

        @Test
        @DisplayName("handleResourceNotFound - recurso inexistente - 404 com resource-not-found")
        void shouldMapResourceNotFound() {
            ProblemDetail problem =
                    handler.handleResourceNotFound(ResourceNotFoundException.user(42L), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(problem.getType())
                    .hasToString(BASE_URI + "/problems/resource-not-found");
            assertThat(problem.getTitle()).isEqualTo("Recurso não encontrado");
            assertThat(problem.getDetail()).contains("42");
        }

        @Test
        @DisplayName("handleDuplicateResource - e-mail repetido - 409 com duplicate-resource")
        void shouldMapDuplicateResource() {
            ProblemDetail problem = handler.handleDuplicateResource(
                    DuplicateResourceException.email("a@x.com"), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(problem.getType()).hasToString(BASE_URI + "/problems/duplicate-resource");
        }

        @Test
        @DisplayName("handleInvalidPassword - senha atual incorreta - 400 com invalid-password")
        void shouldMapInvalidPassword() {
            ProblemDetail problem = handler.handleInvalidPassword(
                    InvalidPasswordException.currentPasswordDoesNotMatch(), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType()).hasToString(BASE_URI + "/problems/invalid-password");
        }

        @Test
        @DisplayName("handleIllegalArgument - invariante de VO - 400 com invalid-field-format")
        void shouldMapIllegalArgument() {
            ProblemDetail problem = handler.handleIllegalArgument(
                    new IllegalArgumentException("CEP inválido: 123"), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType())
                    .hasToString(BASE_URI + "/problems/invalid-field-format");
        }

        @Test
        @DisplayName("handleForbiddenOperation - autocadastro pedindo ADMIN - 403 com forbidden-operation")
        void shouldMapForbiddenOperation() {
            ProblemDetail problem = handler.handleForbiddenOperation(
                    ForbiddenOperationException.selfRegistrationCannotGrantAdmin(), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problem.getType())
                    .hasToString(BASE_URI + "/problems/forbidden-operation");
            assertThat(problem.getDetail()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("handleAccessDenied - recurso de outro usuário - 403 com access-denied")
        void shouldMapAccessDenied() {
            ProblemDetail problem = handler.handleAccessDenied(request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(problem.getType()).hasToString(BASE_URI + "/problems/access-denied");
        }

        @Test
        @DisplayName("handleUnexpected - exceção não prevista - 500 com internal-error")
        void shouldMapUnexpected() {
            ProblemDetail problem =
                    handler.handleUnexpected(new IllegalStateException("boom"), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problem.getType()).hasToString(BASE_URI + "/problems/internal-error");
        }
    }

    @Nested
    @DisplayName("dois 403 com type diferente")
    class DoisForbidden {

        @Test
        @DisplayName("forbidden-operation e access-denied - mesmo status, type distinto")
        void shouldDistinguishTheTwoForbiddenCases() {
            // É esta distinção que justifica o RFC 7807: o cliente não consegue separar os
            // dois casos pelo status, só pelo type.
            ProblemDetail operation = handler.handleForbiddenOperation(
                    ForbiddenOperationException.selfRegistrationCannotGrantAdmin(), request);
            ProblemDetail ownership = handler.handleAccessDenied(request);

            assertThat(operation.getStatus()).isEqualTo(ownership.getStatus());
            assertThat(operation.getType()).isNotEqualTo(ownership.getType());
        }
    }

    @Nested
    @DisplayName("autenticacao")
    class Autenticacao {

        @Test
        @DisplayName("handleAuthentication - credencial inválida - 401 sem revelar a causa")
        void shouldNotRevealWhetherLoginExists() {
            // A mensagem não pode diferenciar "login inexistente" de "senha errada", sob pena
            // de permitir enumerar logins válidos.
            ProblemDetail problem = handler.handleAuthentication(
                    new BadCredentialsException("Bad credentials"), request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problem.getType())
                    .hasToString(BASE_URI + "/problems/authentication-failed");
            assertThat(problem.getDetail()).isEqualTo("Login ou senha inválidos");
        }
    }

    @Nested
    @DisplayName("corpo da resposta")
    class CorpoDaResposta {

        @Test
        @DisplayName("build - qualquer erro - traz instance e timestamp")
        void shouldCarryInstanceAndTimestamp() {
            ProblemDetail problem =
                    handler.handleResourceNotFound(ResourceNotFoundException.user(1L), request);

            assertThat(problem.getInstance()).hasToString(PATH);
            assertThat(problem.getProperties()).containsEntry("timestamp", FIXED_INSTANT);
        }

        @Test
        @DisplayName("handleValidation - campos inválidos - agrega os erros por campo")
        void shouldAggregateFieldErrors() throws NoSuchMethodException {
            MethodArgumentNotValidException exception = validationFailureOn("email", "login");

            ProblemDetail problem = handler.handleValidation(exception, request);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getType()).hasToString(BASE_URI + "/problems/validation-error");
            assertThat(problem.getProperties()).extractingByKey("errors")
                    .isEqualTo(Map.of("email", "E-mail inválido", "login", "E-mail inválido"));
        }

        @Test
        @DisplayName("handleValidation - sem erro de campo - mapa de errors vazio")
        void shouldReturnEmptyErrorsWhenNoFieldError() throws NoSuchMethodException {
            ProblemDetail problem = handler.handleValidation(validationFailureOn(), request);

            assertThat(problem.getProperties()).extractingByKey("errors").isEqualTo(Map.of());
        }
    }

    private MethodArgumentNotValidException validationFailureOn(String... fields)
            throws NoSuchMethodException {

        BindingResult bindingResult = new BeanPropertyBindingResult(new Target(""), "target");
        for (String field : fields) {
            bindingResult.rejectValue(null, "invalid", "E-mail inválido");
            bindingResult.addError(new org.springframework.validation.FieldError(
                    "target", field, "E-mail inválido"));
        }

        MethodParameter parameter =
                new MethodParameter(Target.class.getDeclaredMethod("accept", List.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    private record Target(@NotBlank String email) {

        @SuppressWarnings("unused")
        static void accept(List<String> values) {
            // apenas para obter um MethodParameter válido no teste
        }
    }
}
