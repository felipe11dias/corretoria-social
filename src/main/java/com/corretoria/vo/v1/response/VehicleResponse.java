package com.corretoria.vo.v1.response;

import com.corretoria.enums.FuelType;
import com.corretoria.enums.TransmissionType;
import com.corretoria.enums.VehicleCondition;
import com.corretoria.enums.VehicleType;

/** Detalhamento de automóvel de um anúncio. */
public record VehicleResponse(
        VehicleType vehicleType,
        String brand,
        String model,
        String color,
        Integer manufactureYear,
        Integer modelYear,
        Integer mileageKm,
        FuelType fuelType,
        TransmissionType transmission,
        VehicleCondition condition) {
}
