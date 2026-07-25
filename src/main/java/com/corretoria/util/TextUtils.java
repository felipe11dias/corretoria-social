package com.corretoria.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalização de texto.
 *
 * <p>Utilitário folha: não depende de nenhum pacote do projeto, para não criar ciclo.
 */
public final class TextUtils {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private TextUtils() {
        throw new AssertionError("Classe utilitária não deve ser instanciada");
    }

    /** Remove espaços das pontas; {@code null} vira string vazia. */
    public static String trimToEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * Forma canônica de um texto para comparação: sem espaços nas pontas e em minúsculas.
     *
     * <p>Usa {@link Locale#ROOT} de propósito — com o locale padrão da JVM, um servidor em
     * {@code tr-TR} converteria {@code "I"} para {@code "ı"} e quebraria a comparação.
     */
    public static String normalize(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    /** Remove acentos preservando as letras: {@code "João"} vira {@code "Joao"}. */
    public static String removeAccents(String value) {
        String decomposed = Normalizer.normalize(trimToEmpty(value), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }

    /** Mantém apenas dígitos: {@code "60000-000"} vira {@code "60000000"}. */
    public static String digitsOnly(String value) {
        return NON_DIGITS.matcher(trimToEmpty(value)).replaceAll("");
    }
}
