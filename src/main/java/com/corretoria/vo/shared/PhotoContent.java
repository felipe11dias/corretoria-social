package com.corretoria.vo.shared;

import com.corretoria.util.ObjectUtils;
import com.corretoria.util.TextUtils;
import java.util.Map;

/**
 * Conteúdo binário de uma foto de anúncio, já desacoplado do transporte.
 *
 * <p><strong>Invariante:</strong> bytes presentes e tipo de mídia entre os formatos aceitos.
 *
 * <p>Existe para que o {@code multipart} do controller não vaze para dentro do negócio: a
 * camada de serviço não conhece {@code MultipartFile}, só este valor. A extensão do arquivo é
 * derivada do tipo de mídia, e não do nome enviado pelo cliente — nome de arquivo é dado do
 * usuário e serviria para forjar a extensão do objeto gravado no storage.
 */
public record PhotoContent(byte[] content, String contentType) {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    public PhotoContent {
        ObjectUtils.requireNonNull(content, "O arquivo da foto é obrigatório");
        if (content.length == 0) {
            throw new IllegalArgumentException("O arquivo da foto está vazio");
        }
        contentType = TextUtils.normalize(contentType);
        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(contentType)) {
            throw new IllegalArgumentException(
                    "Formato de foto não suportado: " + contentType
                            + ". Aceitos: image/jpeg, image/png, image/webp");
        }
        content = content.clone();
    }

    /** Extensão correspondente ao tipo de mídia, sem ponto: {@code jpg}, {@code png}, {@code webp}. */
    public String extension() {
        return EXTENSION_BY_CONTENT_TYPE.get(contentType);
    }

    /**
     * Cópia dos bytes.
     *
     * <p>Sem a cópia, o acessor gerado pelo {@code record} devolveria o próprio array e quem
     * recebesse poderia alterá-lo — o valor deixaria de ser imutável.
     */
    @Override
    public byte[] content() {
        return content.clone();
    }
}
