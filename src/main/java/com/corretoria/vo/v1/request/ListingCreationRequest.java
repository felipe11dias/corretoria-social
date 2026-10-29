package com.corretoria.vo.v1.request;

import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Publicação de um anúncio.
 *
 * <p>O corretor dono <strong>não</strong> faz parte do contrato: é resolvido a partir do
 * token. Aceitar o id do dono no corpo permitiria publicar em nome de outro corretor.
 *
 * <p>Exatamente um detalhamento deve vir preenchido, o correspondente ao {@code type} — a
 * coerência é verificada no serviço, que é onde a regra pode enxergar os dois campos juntos.
 */
public record ListingCreationRequest(

        @NotNull(message = "O tipo do anúncio é obrigatório")
        ListingType type,

        @NotNull(message = "O tipo de operação é obrigatório")
        OperationType operationType,

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String title,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
        String description,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
        BigDecimal price,

        @NotBlank(message = "O bairro é obrigatório")
        @Size(max = 255, message = "O bairro deve ter no máximo 255 caracteres")
        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 255, message = "A cidade deve ter no máximo 255 caracteres")
        String city,

        @NotBlank(message = "A UF é obrigatória")
        @Size(min = 2, max = 2, message = "A UF deve ter 2 caracteres")
        String state,

        @NotNull(message = "A latitude é obrigatória")
        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90")
        BigDecimal latitude,

        @NotNull(message = "A longitude é obrigatória")
        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180")
        BigDecimal longitude,

        @Valid
        PropertyRequest property,

        @Valid
        VehicleRequest vehicle) {
}
