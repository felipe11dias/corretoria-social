package com.corretoria.vo.v1.response;

import com.corretoria.enums.PropertyAmenity;
import com.corretoria.enums.PropertyType;
import java.math.BigDecimal;
import java.util.Set;

/** Detalhamento de imóvel de um anúncio. */
public record PropertyResponse(
        PropertyType propertyType,
        BigDecimal totalArea,
        Integer bedrooms,
        Integer bathrooms,
        Integer garageSpots,
        BigDecimal condominiumFee,
        BigDecimal iptu,
        Set<PropertyAmenity> amenities) {
}
