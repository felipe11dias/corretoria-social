package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.corretoria.vo.v1.response.UserResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece o VO de saída com os links de navegação.
 *
 * <p>Os links são montados a partir dos próprios métodos do controller
 * ({@code methodOn}), e não de strings: uma mudança de rota se propaga sozinha para as
 * respostas, em vez de deixar um caminho literal desatualizado.
 *
 * <p>Envolver o VO em {@code EntityModel} preserva a imutabilidade do {@code record} — os
 * links moram no envelope, não dentro do contrato.
 */
@Component
public class UserModelAssembler
        implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {

    private static final String USERS_RELATION = "users";

    @Override
    @NonNull
    public EntityModel<UserResponse> toModel(@NonNull UserResponse user) {
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).findById(user.id())).withSelfRel(),
                linkTo(UserController.class).withRel(USERS_RELATION));
    }
}
