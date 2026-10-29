package com.corretoria.vo.v1.request;

import com.corretoria.enums.PropertyAmenity;
import com.corretoria.enums.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Detalhamento de imóvel dentro de um anúncio.
 *
 * <p>Obrigatório quando {@code type = PROPERTY} e proibido caso contrário — coerência
 * verificada no serviço, porque depende de outro campo da requisição.
 */
public record PropertyRequest(

        @NotNull(message = "O tipo de imóvel é obrigatório")
        PropertyType propertyType,

        @NotNull(message = "A área total é obrigatória")
        @DecimalMin(value = "0.0", inclusive = false, message = "A área total deve ser maior que zero")
        BigDecimal totalArea,

        @PositiveOrZero(message = "O número de quartos não pode ser negativo")
        Integer bedrooms,

        @PositiveOrZero(message = "O número de banheiros não pode ser negativo")
        Integer bathrooms,

        @PositiveOrZero(message = "O número de vagas não pode ser negativo")
        Integer garageSpots,

        @PositiveOrZero(message = "O condomínio não pode ser negativo")
        BigDecimal condominiumFee,

        @PositiveOrZero(message = "O IPTU não pode ser negativo")
        BigDecimal iptu,

        Set<PropertyAmenity> amenities) {

    /**
     * Ausência de comodidades é conjunto vazio, nunca {@code null}.
     *
     * <p>Sem isso, um anúncio sem comodidade nenhuma zeraria a coleção da entidade e o
     * {@code orphanRemoval} do Hibernate perderia a referência gerenciada.
     */
    public PropertyRequest {
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
    }
}
