package com.corretoria.vo.v1.request;

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.PropertyType;
import com.corretoria.enums.VehicleType;
import java.math.BigDecimal;

/**
 * Critérios de busca do catálogo, vindos da query string.
 *
 * <p>Todo campo é opcional: ausente significa "qualquer valor". É um VO de entrada como
 * qualquer outro — o nome {@code ...SearchRequest} descreve a ação (busca), e não colide com a
 * entidade de busca salva, que se chama {@code SavedSearch} justamente por isso.
 *
 * <p><strong>Invariantes:</strong> raio exige coordenada; faixa de preço não pode ser
 * invertida. As duas são verificadas aqui, e não no serviço, porque dependem apenas dos
 * próprios campos.
 *
 * @param status filtro de situação; ausente significa apenas anúncios ativos. Consultar
 *               situação diferente de {@code ACTIVE} exige ser o dono ou administrador,
 *               regra verificada no serviço
 */
public record ListingSearchRequest(

        ListingType type,
        OperationType operationType,
        ListingStatus status,
        Long realtorId,
        String city,
        String state,
        String neighborhood,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        PropertyType propertyType,
        Integer minBedrooms,
        BigDecimal minTotalArea,
        VehicleType vehicleType,
        String brand,
        Integer maxMileageKm,
        Integer minManufactureYear,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radiusKm) {

    public ListingSearchRequest {
        if (radiusKm != null && (latitude == null || longitude == null)) {
            throw new IllegalArgumentException(
                    "A busca por raio exige latitude e longitude");
        }
        if (radiusKm != null && radiusKm <= 0) {
            throw new IllegalArgumentException("O raio deve ser maior que zero");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "O preço mínimo não pode ser maior que o preço máximo");
        }
    }

    /** Situação efetiva da busca: sem filtro explícito, o catálogo mostra o que está ativo. */
    public ListingStatus effectiveStatus() {
        return status == null ? ListingStatus.ACTIVE : status;
    }
}
