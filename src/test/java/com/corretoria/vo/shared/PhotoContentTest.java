package com.corretoria.vo.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoContentTest {

    private static final byte[] BYTES = "conteudo-da-foto".getBytes(StandardCharsets.UTF_8);

    @Nested
    @DisplayName("Invariante")
    class Invariante {

        @ParameterizedTest
        @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
        @DisplayName("PhotoContent - tipo de mídia aceito - constrói")
        void shouldAcceptSupportedContentTypes(String contentType) {
            assertThat(new PhotoContent(BYTES, contentType).contentType()).isEqualTo(contentType);
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/pdf", "text/html", "image/gif", "image/svg+xml"})
        @DisplayName("PhotoContent - tipo de mídia não suportado - lança IllegalArgumentException")
        void shouldRejectUnsupportedContentType(String contentType) {
            // image/svg+xml fica de fora de proposito: SVG carrega script e seria servido do
            // mesmo dominio do bucket publico.
            assertThatThrownBy(() -> new PhotoContent(BYTES, contentType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Formato de foto não suportado");
        }

        @Test
        @DisplayName("PhotoContent - conteúdo nulo - lança")
        void shouldRejectNullContent() {
            assertThatThrownBy(() -> new PhotoContent(null, "image/png"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("PhotoContent - conteúdo vazio - lança IllegalArgumentException")
        void shouldRejectEmptyContent() {
            assertThatThrownBy(() -> new PhotoContent(new byte[0], "image/png"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vazio");
        }
    }

    @Nested
    @DisplayName("Normalizacao")
    class Normalizacao {

        @Test
        @DisplayName("PhotoContent - tipo de mídia com espaços e maiúsculas - normaliza")
        void shouldNormalizeContentType() {
            assertThat(new PhotoContent(BYTES, "  IMAGE/JPEG ").contentType())
                    .isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("extension - deriva a extensão do tipo de mídia, não do nome do arquivo")
        void shouldDeriveExtensionFromContentType() {
            assertThat(new PhotoContent(BYTES, "image/jpeg").extension()).isEqualTo("jpg");
            assertThat(new PhotoContent(BYTES, "image/png").extension()).isEqualTo("png");
            assertThat(new PhotoContent(BYTES, "image/webp").extension()).isEqualTo("webp");
        }

        @Test
        @DisplayName("content - devolve cópia: alterar o retorno não altera o valor")
        void shouldReturnDefensiveCopy() {
            byte[] original = BYTES.clone();
            PhotoContent photo = new PhotoContent(original, "image/png");

            original[0] = 0;
            photo.content()[1] = 0;

            assertThat(photo.content()).isEqualTo(BYTES);
        }
    }
}
