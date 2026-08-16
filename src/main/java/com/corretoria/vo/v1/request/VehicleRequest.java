package com.corretoria.vo.v1.request;

import com.corretoria.enums.FuelType;
import com.corretoria.enums.TransmissionType;
import com.corretoria.enums.VehicleCondition;
import com.corretoria.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Detalhamento de automóvel dentro de um anúncio.
 *
 * <p>Obrigatório quando {@code type = VEHICLE} e proibido caso contrário. Placa e chassi não
 * fazem parte do contrato: identificariam o bem em um catálogo público.
 */
public record VehicleRequest(

        @NotNull(message = "O tipo de veículo é obrigatório")
        VehicleType vehicleType,

        @NotBlank(message = "A marca é obrigatória")
        @Size(max = 255, message = "A marca deve ter no máximo 255 caracteres")
        String brand,

        @NotBlank(message = "O modelo é obrigatório")
        @Size(max = 255, message = "O modelo deve ter no máximo 255 caracteres")
        String model,

        @NotBlank(message = "A cor é obrigatória")
        @Size(max = 255, message = "A cor deve ter no máximo 255 caracteres")
        String color,

        @NotNull(message = "O ano de fabricação é obrigatório")
        @Min(value = 1900, message = "O ano de fabricação deve ser a partir de 1900")
        Integer manufactureYear,

        @NotNull(message = "O ano do modelo é obrigatório")
        @Min(value = 1900, message = "O ano do modelo deve ser a partir de 1900")
        Integer modelYear,

        @NotNull(message = "A quilometragem é obrigatória")
        @PositiveOrZero(message = "A quilometragem não pode ser negativa")
        Integer mileageKm,

        @NotNull(message = "O combustível é obrigatório")
        FuelType fuelType,

        @NotNull(message = "O câmbio é obrigatório")
        TransmissionType transmission,

        @NotNull(message = "O estado de conservação é obrigatório")
        VehicleCondition condition) {
}
