package com.corretoria.exception;

import java.net.URI;

/**
 * Registro dos tipos de problema da API (RFC 7807).
 *
 * <p>O campo {@code type} é a chave de desambiguação do padrão: dois erros distintos podem
 * compartilhar o mesmo {@code status} — há três casos de {@code 400} e dois de {@code 403} —
 * e é o {@code type} que permite ao cliente tratá-los de forma diferente sem depender do
 * {@code title}, que é legível por humanos e pode mudar de redação.
 *
 * <p>A URI é <strong>identificador estável</strong>, não endereço obrigatório de download.
 * Alterar o {@code type} de um erro já publicado é quebra de contrato e exige nova versão
 * da API.
 */
public enum ProblemType {

    VALIDATION_ERROR("validation-error", "Requisição inválida"),
    INVALID_FIELD_FORMAT("invalid-field-format", "Requisição inválida"),
    INVALID_LISTING("invalid-listing", "Anúncio inválido"),
    INVALID_SAVED_SEARCH("invalid-saved-search", "Busca salva inválida"),
    INVALID_PASSWORD("invalid-password", "Senha inválida"),
    AUTHENTICATION_FAILED("authentication-failed", "Falha na autenticação"),
    MISSING_TOKEN("missing-token", "Não autenticado"),
    FORBIDDEN_OPERATION("forbidden-operation", "Operação não permitida"),
    ACCESS_DENIED("access-denied", "Acesso negado"),
    RESOURCE_NOT_FOUND("resource-not-found", "Recurso não encontrado"),
    DUPLICATE_RESOURCE("duplicate-resource", "Conflito de dados"),
    INTERNAL_ERROR("internal-error", "Erro inesperado");

    private static final String PATH = "/problems/";

    private final String slug;
    private final String title;

    ProblemType(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    /** Monta a URI do tipo sob o prefixo configurado em {@code corretoria.problem.base-uri}. */
    public URI uri(String baseUri) {
        return URI.create(baseUri + PATH + slug);
    }
}
