package com.corretoria.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Padroniza <strong>todas</strong> as respostas de erro no formato ProblemDetail (RFC 7807).
 *
 * <p>Centralizar aqui é o que permite aos serviços lançarem exceção de domínio sem conhecer
 * código de status nem montar resposta HTTP — a camada de negócio não sabe que existe HTTP.
 *
 * <p>Erros levantados <em>antes</em> do controller, dentro da cadeia de filtros de segurança,
 * não chegam a este advice: são tratados pelo {@code JwtAuthenticationEntryPoint} da Etapa 9,
 * que reproduz o mesmo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String ERRORS_PROPERTY = "errors";
    private static final String VALIDATION_DETAIL = "Um ou mais campos são inválidos";
    private static final String INTERNAL_DETAIL =
            "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    private final String problemBaseUri;
    private final Clock clock;

    public GlobalExceptionHandler(
            @Value("${corretoria.problem.base-uri}") String problemBaseUri, Clock clock) {
        this.problemBaseUri = problemBaseUri;
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        ProblemDetail problem = build(
                HttpStatus.BAD_REQUEST, ProblemType.VALIDATION_ERROR, VALIDATION_DETAIL, request);
        problem.setProperty(ERRORS_PROPERTY, fieldErrorsOf(exception));
        return problem;
    }

    /** Origem típica: invariante de VO de valor ({@code Email}, {@code ZipCode}). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {

        log.debug("Argumento inválido em {}", request.getRequestURI(), exception);
        return build(HttpStatus.BAD_REQUEST, ProblemType.INVALID_FIELD_FORMAT,
                exception.getMessage(), request);
    }

    /**
     * Combinação de campos que não existe no domínio do anúncio.
     *
     * <p>Tem {@code type} próprio, e não o de validação: o cliente precisa distinguir "faltou
     * um campo" de "essa combinação não existe" para saber o que corrigir.
     */
    @ExceptionHandler(InvalidListingException.class)
    public ProblemDetail handleInvalidListing(
            InvalidListingException exception, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ProblemType.INVALID_LISTING,
                exception.getMessage(), request);
    }

    /**
     * Avaliação sem negociação prévia.
     *
     * <p>{@code 403} com type próprio: diferente de acesso negado, esta porta se abre sozinha
     * assim que houver uma proposta entre as partes.
     */
    @ExceptionHandler(ReviewNotAllowedException.class)
    public ProblemDetail handleReviewNotAllowed(
            ReviewNotAllowedException exception, HttpServletRequest request) {

        return build(HttpStatus.FORBIDDEN, ProblemType.REVIEW_NOT_ALLOWED,
                exception.getMessage(), request);
    }

    /** Ver {@link #handleInvalidListing}: mesma distinção, aplicada à negociação. */
    @ExceptionHandler(InvalidProposalException.class)
    public ProblemDetail handleInvalidProposal(
            InvalidProposalException exception, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ProblemType.INVALID_PROPOSAL,
                exception.getMessage(), request);
    }

    /** Ver {@link #handleInvalidListing}: mesma distinção, aplicada à busca salva. */
    @ExceptionHandler(InvalidSavedSearchException.class)
    public ProblemDetail handleInvalidSavedSearch(
            InvalidSavedSearchException exception, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ProblemType.INVALID_SAVED_SEARCH,
                exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(
            InvalidPasswordException exception, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ProblemType.INVALID_PASSWORD,
                exception.getMessage(), request);
    }

    /**
     * Credenciais inválidas.
     *
     * <p>A mensagem é sempre a mesma, independentemente de o login não existir ou de a senha
     * estar errada: distinguir os dois casos permitiria enumerar logins válidos.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {

        log.debug("Falha de autenticação em {}", request.getRequestURI(), exception);
        return build(HttpStatus.UNAUTHORIZED, ProblemType.AUTHENTICATION_FAILED,
                "Login ou senha inválidos", request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperation(
            ForbiddenOperationException exception, HttpServletRequest request) {

        return build(HttpStatus.FORBIDDEN, ProblemType.FORBIDDEN_OPERATION,
                exception.getMessage(), request);
    }

    /** Recurso de outro usuário: falha de posse, barrada pelo {@code @PreAuthorize}. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ProblemType.ACCESS_DENIED,
                "Você não tem permissão para acessar este recurso", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {

        return build(HttpStatus.NOT_FOUND, ProblemType.RESOURCE_NOT_FOUND,
                exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(
            DuplicateResourceException exception, HttpServletRequest request) {

        return build(HttpStatus.CONFLICT, ProblemType.DUPLICATE_RESOURCE,
                exception.getMessage(), request);
    }

    /**
     * Rota inexistente.
     *
     * <p>Precisa vir <strong>antes</strong> do handler genérico: sem ela, o Spring lança
     * {@code NoResourceFoundException} para um caminho desconhecido, o {@code catch}
     * genérico transforma em {@code 500}, e um erro de digitação na URL do cliente vira
     * "erro do servidor" — além de poluir o log de erro com ruído.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ProblemType.RESOURCE_NOT_FOUND,
                "Recurso não encontrado: " + request.getRequestURI(), request);
    }

    /**
     * Rede de segurança para o que não foi previsto.
     *
     * <p>O detalhe devolvido é genérico de propósito — mensagem de exceção pode carregar
     * nome de tabela, trecho de SQL ou caminho de arquivo. A causa real vai para o log, com
     * o caminho da requisição para correlacionar.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}",
                request.getMethod(), request.getRequestURI(), exception);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, ProblemType.INTERNAL_ERROR,
                INTERNAL_DETAIL, request);
    }

    private ProblemDetail build(
            HttpStatus status, ProblemType type, String detail, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type.uri(problemBaseUri));
        problem.setTitle(type.title());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now(clock));
        return problem;
    }

    private Map<String, String> fieldErrorsOf(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errors;
    }
}
