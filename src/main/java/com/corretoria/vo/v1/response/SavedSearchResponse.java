package com.corretoria.vo.v1.response;

import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.SearchVisibility;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representação de uma busca salva.
 *
 * <p>O mesmo contrato serve ao alerta privado do dono e à vitrine de procura vista pelos
 * corretores — a diferença é o {@code visibility}, e quem não é o dono só alcança as públicas.
 */
public record SavedSearchResponse(
        Long id,
        ListingType type,
        SearchVisibility visibility,
        String title,
        boolean active,
        OperationType operationType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String city,
        String state,
        String neighborhood,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radiusKm,
        CustomerSummaryResponse customer,
        PropertySearchCriteriaResponse propertyCriteria,
        VehicleSearchCriteriaResponse vehicleCriteria,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt) {
}
