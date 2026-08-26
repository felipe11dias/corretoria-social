package com.corretoria.mapper;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingFavorite;
import com.corretoria.entity.RealtorFollow;
import com.corretoria.entity.User;
import com.corretoria.vo.v1.response.ListingFavoriteResponse;
import com.corretoria.vo.v1.response.ListingSummaryResponse;
import com.corretoria.vo.v1.response.RealtorFollowResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import org.mapstruct.Mapper;

/**
 * Tradução dos vínculos sociais para os VOs de contrato.
 *
 * <p>Favorito e seguir compartilham um mapeador porque compartilham a natureza: são junções
 * puras, sem lado de entrada — ninguém envia corpo para favoritar, o recurso é a própria
 * relação entre quem chama e o que está na rota. Um mapeador por vínculo seriam duas classes
 * de três linhas cada.
 */
@Mapper
public interface SocialMapper {

    ListingFavoriteResponse toResponse(ListingFavorite favorite);

    RealtorFollowResponse toResponse(RealtorFollow follow);

    ListingSummaryResponse toListingSummary(Listing listing);

    RealtorSummaryResponse toRealtorSummary(User realtor);
}
