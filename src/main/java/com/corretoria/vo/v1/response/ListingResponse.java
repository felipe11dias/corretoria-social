package com.corretoria.vo.v1.response;

import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representação pública de um anúncio.
 *
 * <p>Exatamente um entre {@code property} e {@code vehicle} vem preenchido, conforme o
 * {@code type}.
 *
 * @param publishedAt data de publicação — é o {@code createdAt} da entidade, já que publicar
 *                   é imediato e não existe fila de aprovação
 */
public record ListingResponse(
        Long id,
        ListingType type,
        OperationType operationType,
        ListingStatus status,
        String title,
        String description,
        BigDecimal price,
        String neighborhood,
        String city,
        String state,
        BigDecimal latitude,
        BigDecimal longitude,
        RealtorSummaryResponse realtor,
        PropertyResponse property,
        VehicleResponse vehicle,
        List<ListingPhotoResponse> photos,
        LocalDateTime publishedAt,
        LocalDateTime lastUpdatedAt) {
}
