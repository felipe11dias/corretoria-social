package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.corretoria.vo.v1.response.ReviewResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece a avaliação com o link para o autor.
 *
 * <p>Não há {@code self} porque não existe rota de avaliação individual: a avaliação é lida na
 * coleção do corretor, que é onde ela faz sentido — uma nota isolada não é reputação.
 */
@Component
public class ReviewModelAssembler
        implements RepresentationModelAssembler<ReviewResponse, EntityModel<ReviewResponse>> {

    private static final String CUSTOMER_RELATION = "customer";

    @Override
    @NonNull
    public EntityModel<ReviewResponse> toModel(@NonNull ReviewResponse review) {
        return EntityModel.of(review,
                linkTo(methodOn(UserController.class).findById(review.customer().id()))
                        .withRel(CUSTOMER_RELATION));
    }
}
