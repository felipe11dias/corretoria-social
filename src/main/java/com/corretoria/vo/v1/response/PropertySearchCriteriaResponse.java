package com.corretoria.vo.v1.response;

import com.corretoria.enums.PropertyType;
import java.math.BigDecimal;

/** Critérios de imóvel de uma busca salva. */
public record PropertySearchCriteriaResponse(
        PropertyType propertyType,
        Integer minBedrooms,
        Integer minBathrooms,
        BigDecimal minTotalArea) {
}
