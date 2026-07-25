package com.corretoria.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextUtilsTest {

    @Nested
    @DisplayName("trimToEmpty")
    class TrimToEmpty {

        @Test
        @DisplayName("trimToEmpty - nulo - devolve string vazia")
        void shouldConvertNullToEmpty() {
            assertThat(TextUtils.trimToEmpty(null)).isEmpty();
        }

        @Test
        @DisplayName("trimToEmpty - texto com espaços - remove das duas pontas")
        void shouldStripBothEnds() {
            assertThat(TextUtils.trimToEmpty("  Fortaleza  ")).isEqualTo("Fortaleza");
        }
    }

    @Nested
    @DisplayName("normalize")
    class Normalize {

        @Test
        @DisplayName("normalize - texto misto - devolve minúsculo e sem espaços")
        void shouldLowerCaseAndStrip() {
            assertThat(TextUtils.normalize("  JoAo@Email.COM ")).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("normalize - I maiúsculo - usa Locale.ROOT e não o do sistema")
        void shouldUseRootLocale() {
            // Com o locale turco, "I".toLowerCase() vira "ı" (i sem ponto) e quebraria
            // qualquer comparação de e-mail ou login.
            assertThat(TextUtils.normalize("ILHA")).isEqualTo("ilha");
        }
    }

    @Nested
    @DisplayName("removeAccents")
    class RemoveAccents {

        @Test
        @DisplayName("removeAccents - texto acentuado - preserva as letras sem os acentos")
        void shouldRemoveDiacritics() {
            assertThat(TextUtils.removeAccents("João Conceição")).isEqualTo("Joao Conceicao");
        }

        @Test
        @DisplayName("removeAccents - texto sem acento - devolve inalterado")
        void shouldKeepPlainText() {
            assertThat(TextUtils.removeAccents("Fortaleza")).isEqualTo("Fortaleza");
        }
    }

    @Nested
    @DisplayName("digitsOnly")
    class DigitsOnly {

        @Test
        @DisplayName("digitsOnly - CEP com máscara - devolve só os dígitos")
        void shouldKeepOnlyDigits() {
            assertThat(TextUtils.digitsOnly("60000-000")).isEqualTo("60000000");
        }

        @Test
        @DisplayName("digitsOnly - texto sem dígito - devolve string vazia")
        void shouldReturnEmptyWhenNoDigits() {
            assertThat(TextUtils.digitsOnly("sem numero")).isEmpty();
        }
    }
}
