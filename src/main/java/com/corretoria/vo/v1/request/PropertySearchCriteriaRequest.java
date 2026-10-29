package com.corretoria.vo.v1.request;

import com.corretoria.enums.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Critérios de imóvel de uma busca salva.
 *
 * <p>Todo campo é opcional: ausente significa "qualquer valor". Só faz sentido quando a busca
 * é do tipo {@code PROPERTY} — a coerência é verificada no serviço.
 */
public record PropertySearchCriteriaRequest(

        PropertyType propertyType,

        @PositiveOrZero(message = "O mínimo de quartos não pode ser negativo")
        Integer minBedrooms,

        @PositiveOrZero(message = "O mínimo de banheiros não pode ser negativo")
        Integer minBathrooms,

        @DecimalMin(value = "0.0", inclusive = false,
                message = "A área mínima deve ser maior que zero")
        BigDecimal minTotalArea) {
}
