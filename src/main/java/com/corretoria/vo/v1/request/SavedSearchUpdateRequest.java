package com.corretoria.vo.v1.request;

import com.corretoria.enums.OperationType;
import com.corretoria.enums.SearchVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Atualização de uma busca salva.
 *
 * <p>O {@code type} não é alterável, pelo mesmo motivo do anúncio: trocar imóvel por automóvel
 * descartaria os critérios específicos inteiros — é criar outra busca, não editar esta.
 *
 * <p>{@code active} entra aqui: pausar o alerta é edição da busca, não uma transição de estado
 * separada como a situação do anúncio.
 */
public record SavedSearchUpdateRequest(

        @NotNull(message = "A visibilidade da busca é obrigatória")
        SearchVisibility visibility,

        @NotBlank(message = "O título da busca é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String title,

        @NotNull(message = "Informe se a busca continua ativa")
        Boolean active,

        OperationType operationType,

        @PositiveOrZero(message = "O preço mínimo não pode ser negativo")
        BigDecimal minPrice,

        @PositiveOrZero(message = "O preço máximo não pode ser negativo")
        BigDecimal maxPrice,

        @Size(max = 255, message = "A cidade deve ter no máximo 255 caracteres")
        String city,

        @Size(min = 2, max = 2, message = "A UF deve ter 2 caracteres")
        String state,

        @Size(max = 255, message = "O bairro deve ter no máximo 255 caracteres")
        String neighborhood,

        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180")
        BigDecimal longitude,

        @Positive(message = "O raio deve ser maior que zero")
        Integer radiusKm,

        @Valid
        PropertySearchCriteriaRequest propertyCriteria,

        @Valid
        VehicleSearchCriteriaRequest vehicleCriteria) {
}
