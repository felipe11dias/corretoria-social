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
 * <p>A coerência entre os critérios — raio exige coordenada, faixa de preço não invertida — é
 * verificada no serviço, e não no construtor compacto como nos VOs de valor
 * ({@code Email}, {@code ZipCode}). Este VO é montado pelo <em>data binding</em> da query
 * string: uma exceção lançada durante a construção sairia como falha de instanciação do
 * Spring, e o cliente receberia {@code 500} onde o certo é {@code 400}.
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

    /** Situação efetiva da busca: sem filtro explícito, o catálogo mostra o que está ativo. */
    public ListingStatus effectiveStatus() {
        return status == null ? ListingStatus.ACTIVE : status;
    }
}
