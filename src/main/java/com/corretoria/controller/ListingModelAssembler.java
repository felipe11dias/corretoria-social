package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.corretoria.vo.v1.response.ListingResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece o anúncio com os links de navegação.
 *
 * <p>Mesmo padrão do {@code UserModelAssembler}: links montados a partir dos métodos do
 * controller, nunca de caminhos literais.
 *
 * <p>Além do próprio anúncio, aponta para o corretor dono — é o caminho natural de quem lê o
 * catálogo e quer saber com quem está falando.
 */
@Component
public class ListingModelAssembler
        implements RepresentationModelAssembler<ListingResponse, EntityModel<ListingResponse>> {

    private static final String LISTINGS_RELATION = "listings";
    private static final String REALTOR_RELATION = "realtor";

    @Override
    @NonNull
    public EntityModel<ListingResponse> toModel(@NonNull ListingResponse listing) {
        return EntityModel.of(listing,
                linkTo(methodOn(ListingController.class).findById(listing.id())).withSelfRel(),
                linkTo(ListingController.class).withRel(LISTINGS_RELATION),
                linkTo(methodOn(UserController.class).findById(listing.realtor().id()))
                        .withRel(REALTOR_RELATION));
    }
}
