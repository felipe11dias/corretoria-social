package com.corretoria.mapper;

import com.corretoria.entity.RealtorPost;
import com.corretoria.entity.RealtorPostComment;
import com.corretoria.entity.User;
import com.corretoria.vo.v1.request.RealtorPostCommentCreationRequest;
import com.corretoria.vo.v1.request.RealtorPostCreationRequest;
import com.corretoria.vo.v1.response.RealtorPostCommentResponse;
import com.corretoria.vo.v1.response.RealtorPostResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import com.corretoria.vo.v1.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Tradução entre o agregado {@link RealtorPost} e os VOs de contrato.
 *
 * <p>O total de curtidas chega como parâmetro, e não por injeção do repository: o mapeador
 * conhece entidade, VO e enum -- contar é trabalho de quem consulta. Mesmo raciocínio do
 * prefixo de fotos no {@code ListingMapper}.
 */
@Mapper
public interface RealtorPostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realtor", ignore = true)
    @Mapping(target = "comments", ignore = true)
    RealtorPost toEntity(RealtorPostCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "author", ignore = true)
    RealtorPostComment toEntity(RealtorPostCommentCreationRequest request);

    RealtorPostResponse toResponse(RealtorPost post, long likeCount);

    RealtorPostCommentResponse toCommentResponse(RealtorPostComment comment);

    RealtorSummaryResponse toRealtorSummary(User realtor);

    UserSummaryResponse toUserSummary(User user);
}
