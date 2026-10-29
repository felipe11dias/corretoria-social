package com.corretoria.mapper;

import com.corretoria.entity.PropertySearchCriteria;
import com.corretoria.entity.SavedSearch;
import com.corretoria.entity.User;
import com.corretoria.entity.VehicleSearchCriteria;
import com.corretoria.vo.v1.request.PropertySearchCriteriaRequest;
import com.corretoria.vo.v1.request.SavedSearchCreationRequest;
import com.corretoria.vo.v1.request.SavedSearchUpdateRequest;
import com.corretoria.vo.v1.request.VehicleSearchCriteriaRequest;
import com.corretoria.vo.v1.response.CustomerSummaryResponse;
import com.corretoria.vo.v1.response.SavedSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Tradução entre o agregado {@link SavedSearch} e os VOs de contrato.
 *
 * <p>Não resolve o cliente dono nem o estado inicial de {@code active}: o dono vem do token e a
 * busca nasce ativa. Deixá-los de fora é o que impede o corpo da requisição de escolher em
 * nome de quem procurar.
 */
@Mapper
public interface SavedSearchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "active", ignore = true)
    SavedSearch toEntity(SavedSearchCreationRequest request);

    /**
     * Aplica a edição sobre a busca existente.
     *
     * <p>Os critérios específicos ficam de fora e são atualizados em separado, pelo método do
     * tipo já registrado — mesmo desenho do {@code ListingMapper}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "propertyCriteria", ignore = true)
    @Mapping(target = "vehicleCriteria", ignore = true)
    void updateEntity(@MappingTarget SavedSearch savedSearch, SavedSearchUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "savedSearch", ignore = true)
    PropertySearchCriteria toEntity(PropertySearchCriteriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "savedSearch", ignore = true)
    void updatePropertyCriteria(
            @MappingTarget PropertySearchCriteria criteria, PropertySearchCriteriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "savedSearch", ignore = true)
    VehicleSearchCriteria toEntity(VehicleSearchCriteriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "savedSearch", ignore = true)
    void updateVehicleCriteria(
            @MappingTarget VehicleSearchCriteria criteria, VehicleSearchCriteriaRequest request);

    SavedSearchResponse toResponse(SavedSearch savedSearch);

    CustomerSummaryResponse toCustomerSummary(User customer);
}
