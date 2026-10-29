package com.corretoria.mapper;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingPhoto;
import com.corretoria.entity.Property;
import com.corretoria.entity.User;
import com.corretoria.entity.Vehicle;
import com.corretoria.vo.v1.request.ListingCreationRequest;
import com.corretoria.vo.v1.request.ListingUpdateRequest;
import com.corretoria.vo.v1.request.PropertyRequest;
import com.corretoria.vo.v1.request.VehicleRequest;
import com.corretoria.vo.v1.response.ListingPhotoResponse;
import com.corretoria.vo.v1.response.ListingResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Tradução entre o agregado {@link Listing} e os VOs de contrato.
 *
 * <p>Cobre o agregado inteiro — anúncio, detalhamento e fotos — porque {@code Property},
 * {@code Vehicle} e {@code ListingPhoto} não existem fora dele.
 *
 * <p>O mapeador não resolve o corretor dono nem a situação do anúncio: o dono vem do token e a
 * situação nasce {@code ACTIVE}. Deixar os dois de fora é o que impede um corpo de requisição
 * de escolher em nome de quem publicar.
 *
 * <p>O prefixo público das fotos entra por {@code @Context}, e não por injeção do
 * {@code StorageService}: mapeador conhece entidade, VO e enum — depender de um serviço
 * inverteria a direção da dependência.
 */
@Mapper
public interface ListingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realtor", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "photos", ignore = true)
    Listing toEntity(ListingCreationRequest request);

    /**
     * Aplica a edição sobre o anúncio existente.
     *
     * <p>Tipo, dono, situação, fotos e detalhamento ficam de fora: os quatro primeiros não são
     * editáveis por esta via, e o detalhamento é atualizado em separado, pelo método
     * específico do tipo já registrado no anúncio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realtor", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "property", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    void updateEntity(@MappingTarget Listing listing, ListingUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    Property toEntity(PropertyRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    void updateProperty(@MappingTarget Property property, PropertyRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    void updateVehicle(@MappingTarget Vehicle vehicle, VehicleRequest request);

    @Mapping(target = "publishedAt", source = "createdAt")
    ListingResponse toResponse(Listing listing, @Context String photoBaseUrl);

    RealtorSummaryResponse toRealtorSummary(User realtor);

    /**
     * Monta a URL da foto a partir do prefixo público e da chave do objeto.
     *
     * <p>Escrito à mão, e não declarado para o MapStruct gerar, porque o único campo derivado
     * é a URL — a alternativa seria um método qualificado só para concatenar duas strings.
     */
    default ListingPhotoResponse toPhotoResponse(
            ListingPhoto photo, @Context String photoBaseUrl) {

        if (photo == null) {
            return null;
        }
        return new ListingPhotoResponse(
                photo.getId(),
                photoBaseUrl + "/" + photo.getStorageKey(),
                photo.getPosition(),
                photo.isCover());
    }

    /**
     * Substitui as comodidades reaproveitando a coleção gerenciada.
     *
     * <p>Trocar a instância do {@code Set} faria o Hibernate perder a referência da coleção
     * que ele controla — o mesmo motivo de {@code User.replaceAddresses} limpar a lista em vez
     * de atribuir outra.
     */
    @AfterMapping
    default void replaceAmenities(
            @MappingTarget Property property, PropertyRequest request) {

        property.getAmenities().clear();
        property.getAmenities().addAll(request.amenities());
    }
}
