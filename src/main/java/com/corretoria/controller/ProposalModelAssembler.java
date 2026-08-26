package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.corretoria.vo.v1.response.ProposalResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece a proposta com os links de navegação.
 *
 * <p>O link para mudar a situação só aparece enquanto a proposta está pendente: aceitar,
 * recusar e retirar são definitivos, e oferecer o caminho depois disso seria convidar o cliente
 * da API para um {@code 400}.
 */
@Component
public class ProposalModelAssembler
        implements RepresentationModelAssembler<ProposalResponse, EntityModel<ProposalResponse>> {

    private static final String PROPOSALS_RELATION = "proposals";
    private static final String LISTING_RELATION = "listing";
    private static final String STATUS_RELATION = "status";
    private static final String MESSAGES_RELATION = "messages";

    @Override
    @NonNull
    public EntityModel<ProposalResponse> toModel(@NonNull ProposalResponse proposal) {
        EntityModel<ProposalResponse> model = EntityModel.of(proposal,
                linkTo(methodOn(ProposalController.class).findById(proposal.id())).withSelfRel(),
                linkTo(ProposalController.class).withRel(PROPOSALS_RELATION),
                linkTo(methodOn(ListingController.class).findById(proposal.listing().id()))
                        .withRel(LISTING_RELATION),
                linkTo(ProposalController.class).slash(proposal.id()).slash(MESSAGES_RELATION)
                        .withRel(MESSAGES_RELATION));

        if (proposal.pending()) {
            model.add(linkTo(ProposalController.class).slash(proposal.id()).slash(STATUS_RELATION)
                    .withRel(STATUS_RELATION));
        }
        return model;
    }
}
