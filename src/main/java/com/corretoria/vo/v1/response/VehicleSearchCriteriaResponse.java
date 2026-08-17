package com.corretoria.vo.v1.response;

import com.corretoria.enums.VehicleType;

/** Critérios de automóvel de uma busca salva. */
public record VehicleSearchCriteriaResponse(
        VehicleType vehicleType,
        String brand,
        Integer maxMileageKm,
        Integer minManufactureYear) {
}
