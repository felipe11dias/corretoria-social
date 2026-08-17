package com.corretoria.vo.v1.request;

import com.corretoria.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Critérios de automóvel de uma busca salva.
 *
 * <p>Todo campo é opcional: ausente significa "qualquer valor". Só faz sentido quando a busca
 * é do tipo {@code VEHICLE} — a coerência é verificada no serviço.
 */
public record VehicleSearchCriteriaRequest(

        VehicleType vehicleType,

        @Size(max = 255, message = "A marca deve ter no máximo 255 caracteres")
        String brand,

        @PositiveOrZero(message = "A quilometragem máxima não pode ser negativa")
        Integer maxMileageKm,

        @Min(value = 1900, message = "O ano mínimo de fabricação deve ser a partir de 1900")
        Integer minManufactureYear) {
}
