package com.corretoria.util;

import java.util.Collection;
import java.util.Objects;

/**
 * Verificações de presença usadas como pré-condição de argumento.
 *
 * <p>Utilitário folha: não depende de nenhum pacote do projeto, para não criar ciclo.
 */
public final class ObjectUtils {

    private ObjectUtils() {
        throw new AssertionError("Classe utilitária não deve ser instanciada");
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    public static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    /**
     * Exige um texto com conteúdo e devolve a versão sem espaços nas pontas.
     *
     * <p>Lança {@link IllegalArgumentException} — e não {@code NullPointerException} — porque
     * a origem é sempre dado de entrada do usuário, que o handler global traduz em
     * {@code 400}.
     */
    public static String requireNonBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
