package com.corretoria.mapper;

import com.corretoria.entity.Listing;
import com.corretoria.entity.Proposal;
import com.corretoria.entity.ProposalMessage;
import com.corretoria.entity.User;
import com.corretoria.vo.v1.request.ProposalCreationRequest;
import com.corretoria.vo.v1.request.ProposalMessageCreationRequest;
import com.corretoria.vo.v1.response.CustomerSummaryResponse;
import com.corretoria.vo.v1.response.ListingSummaryResponse;
import com.corretoria.vo.v1.response.ProposalMessageResponse;
import com.corretoria.vo.v1.response.ProposalResponse;
import com.corretoria.vo.v1.response.RealtorSummaryResponse;
import com.corretoria.vo.v1.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Tradução entre o agregado {@link Proposal} e os VOs de contrato.
 *
 * <p>Do contrato para a entidade, o mapeador traduz apenas o valor proposto e o texto da
 * mensagem: anúncio, cliente, corretor, iniciador e busca de origem exigem consulta e decisão
 * de papel, que são do serviço. É o que impede o corpo da requisição de escolher em nome de
 * quem — ou contra quem — negociar.
 */
@Mapper
public interface ProposalMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "originSavedSearch", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "messages", ignore = true)
    Proposal toEntity(ProposalCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "author", ignore = true)
    ProposalMessage toEntity(ProposalMessageCreationRequest request);

    /**
     * O corretor da negociação é o dono do anúncio — não há coluna separada para ele.
     */
    @Mapping(target = "realtor", source = "listing.realtor")
    @Mapping(target = "originSavedSearchId", source = "originSavedSearch.id")
    ProposalResponse toResponse(Proposal proposal);

    ProposalMessageResponse toMessageResponse(ProposalMessage message);

    ListingSummaryResponse toListingSummary(Listing listing);

    CustomerSummaryResponse toCustomerSummary(User customer);

    RealtorSummaryResponse toRealtorSummary(User realtor);

    UserSummaryResponse toUserSummary(User user);
}
