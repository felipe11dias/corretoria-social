package com.corretoria.mapper;

import com.corretoria.entity.Review;
import com.corretoria.entity.User;
import com.corretoria.vo.v1.request.ReviewRequest;
import com.corretoria.vo.v1.response.CustomerSummaryResponse;
import com.corretoria.vo.v1.response.ReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Tradução entre {@link Review} e os VOs de contrato.
 *
 * <p>Não resolve corretor nem cliente: o avaliado vem da rota e o autor vem do token. O método
 * de atualização é o que torna a avaliação um <em>upsert</em> — a segunda avaliação do mesmo
 * par reescreve a primeira.
 */
@Mapper
public interface ReviewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realtor", ignore = true)
    @Mapping(target = "customer", ignore = true)
    Review toEntity(ReviewRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realtor", ignore = true)
    @Mapping(target = "customer", ignore = true)
    void updateEntity(@MappingTarget Review review, ReviewRequest request);

    ReviewResponse toResponse(Review review);

    CustomerSummaryResponse toCustomerSummary(User customer);
}
