package com.corretoria.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObjectUtilsTest {

    @Nested
    @DisplayName("isBlank")
    class IsBlank {

        @Test
        @DisplayName("isBlank - nulo, vazio ou só espaços - verdadeiro")
        void shouldDetectAbsenceOfContent() {
            assertThat(ObjectUtils.isBlank(null)).isTrue();
            assertThat(ObjectUtils.isBlank("")).isTrue();
            assertThat(ObjectUtils.isBlank("   ")).isTrue();
        }

        @Test
        @DisplayName("isBlank - texto com conteúdo - falso")
        void shouldDetectContent() {
            assertThat(ObjectUtils.isBlank("a")).isFalse();
        }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("isEmpty - coleção nula ou vazia - verdadeiro")
        void shouldDetectEmptyCollection() {
            assertThat(ObjectUtils.isEmpty(null)).isTrue();
            assertThat(ObjectUtils.isEmpty(List.of())).isTrue();
        }

        @Test
        @DisplayName("isEmpty - coleção com item - falso")
        void shouldDetectPopulatedCollection() {
            assertThat(ObjectUtils.isEmpty(List.of("a"))).isFalse();
        }
    }

    @Nested
    @DisplayName("requireNonBlank")
    class RequireNonBlank {

        @Test
        @DisplayName("requireNonBlank - texto com espaços - devolve sem as pontas")
        void shouldReturnStrippedValue() {
            assertThat(ObjectUtils.requireNonBlank("  ok  ", "obrigatório")).isEqualTo("ok");
        }

        @Test
        @DisplayName("requireNonBlank - em branco - lança IllegalArgumentException, não NPE")
        void shouldThrowIllegalArgumentException() {
            // O handler global traduz IllegalArgumentException em 400; NullPointerException
            // cairia no 500 genérico, escondendo um erro que é do dado de entrada.
            assertThatThrownBy(() -> ObjectUtils.requireNonBlank(null, "O nome é obrigatório"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O nome é obrigatório");
        }
    }
}
