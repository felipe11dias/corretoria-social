package com.corretoria.vo.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZipCodeTest {

    @Nested
    @DisplayName("normalizacao")
    class Normalizacao {

        @Test
        @DisplayName("ZipCode - CEP com máscara - guarda apenas os 8 dígitos")
        void shouldStripMask() {
            ZipCode zipCode = new ZipCode("60000-000");

            assertThat(zipCode.value()).isEqualTo("60000000");
        }

        @Test
        @DisplayName("ZipCode - CEP sem máscara - mantém os 8 dígitos")
        void shouldKeepDigitsOnlyInput() {
            ZipCode zipCode = new ZipCode("60000000");

            assertThat(zipCode.value()).isEqualTo("60000000");
        }

        @Test
        @DisplayName("ZipCode - com e sem máscara - são iguais por valor")
        void shouldTreatMaskedAndUnmaskedAsEqual() {
            assertThat(new ZipCode("60160-230")).isEqualTo(new ZipCode("60160230"));
        }

        @Test
        @DisplayName("formatted - CEP normalizado - devolve no formato 00000-000")
        void shouldFormatOnDemand() {
            assertThat(new ZipCode("60160230").formatted()).isEqualTo("60160-230");
        }
    }

    @Nested
    @DisplayName("invariante")
    class Invariante {

        @ParameterizedTest
        @ValueSource(strings = {"6016023", "601602301", "60160-23", "abcdefgh"})
        @DisplayName("ZipCode - quantidade de dígitos diferente de 8 - lança IllegalArgumentException")
        void shouldRejectWrongDigitCount(String value) {
            assertThatThrownBy(() -> new ZipCode(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CEP inválido");
        }

        @Test
        @DisplayName("ZipCode - nulo - lança IllegalArgumentException")
        void shouldRejectNull() {
            assertThatThrownBy(() -> new ZipCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatório");
        }
    }
}
