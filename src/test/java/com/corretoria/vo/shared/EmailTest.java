package com.corretoria.vo.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Nested
    @DisplayName("normalizacao")
    class Normalizacao {

        @Test
        @DisplayName("Email - endereço em maiúsculas - normaliza para minúsculas")
        void shouldNormalizeToLowerCase() {
            // arrange + act
            Email email = new Email("Joao.Silva@Email.COM");

            // assert
            assertThat(email.value()).isEqualTo("joao.silva@email.com");
        }

        @Test
        @DisplayName("Email - endereço com espaços nas pontas - remove os espaços")
        void shouldStripSurroundingSpaces() {
            Email email = new Email("  joao@email.com  ");

            assertThat(email.value()).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("Email - mesmos endereços em caixas diferentes - são iguais por valor")
        void shouldTreatDifferentCasesAsEqual() {
            // Esta é a garantia da regra de e-mail único: sem a normalização, os dois
            // endereços abaixo passariam pela checagem de unicidade como distintos.
            Email upperCase = new Email("A@x.com");
            Email lowerCase = new Email("a@x.com");

            assertThat(upperCase).isEqualTo(lowerCase);
            assertThat(upperCase).hasSameHashCodeAs(lowerCase);
        }
    }

    @Nested
    @DisplayName("invariante")
    class Invariante {

        @ParameterizedTest
        @ValueSource(strings = {
                "joao@email.com",
                "joao.silva@email.com.br",
                "joao+tag@email.com",
                "joao_silva@sub.email.com"
        })
        @DisplayName("Email - formato válido - aceita")
        void shouldAcceptValidFormats(String value) {
            assertThat(new Email(value).value()).isEqualTo(value);
        }

        @ParameterizedTest
        @ValueSource(strings = {"joao", "joao@", "@email.com", "joao@email", "joao @email.com"})
        @DisplayName("Email - formato inválido - lança IllegalArgumentException")
        void shouldRejectInvalidFormats(String value) {
            assertThatThrownBy(() -> new Email(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail inválido");
        }

        @Test
        @DisplayName("Email - nulo - lança IllegalArgumentException")
        void shouldRejectNull() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatório");
        }

        @Test
        @DisplayName("Email - em branco - lança IllegalArgumentException")
        void shouldRejectBlank() {
            assertThatThrownBy(() -> new Email("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatório");
        }
    }
}
