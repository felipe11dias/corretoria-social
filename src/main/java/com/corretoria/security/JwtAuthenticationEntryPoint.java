package com.corretoria.security;

import com.corretoria.exception.ProblemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Responde ao acesso sem autenticação válida mantendo o contrato de erro do projeto.
 *
 * <p>Existe porque o {@code GlobalExceptionHandler} <strong>não alcança</strong> este caso: a
 * requisição é barrada dentro da cadeia de filtros, antes de chegar a qualquer controller, e
 * o padrão do Spring seria um corpo vazio ou um HTML de erro. Aqui ela recebe o mesmo
 * {@code ProblemDetail} de todo o resto.
 *
 * <p>Cobre igualmente token ausente, expirado, malformado e assinado por outra chave — todos
 * respondem {@code /problems/missing-token}. Diferenciá-los revelaria a um atacante o
 * resultado de cada tentativa de forjar token.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String DETAIL =
            "Autenticação necessária: envie o cabeçalho Authorization: Bearer <token>";

    private final ObjectMapper objectMapper;
    private final String problemBaseUri;
    private final Clock clock;

    public JwtAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            @Value("${corretoria.problem.base-uri}") String problemBaseUri,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.problemBaseUri = problemBaseUri;
        this.clock = clock;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETAIL);
        problem.setType(ProblemType.MISSING_TOKEN.uri(problemBaseUri));
        problem.setTitle(ProblemType.MISSING_TOKEN.title());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now(clock));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
