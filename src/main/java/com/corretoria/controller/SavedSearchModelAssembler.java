package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.corretoria.vo.v1.response.SavedSearchResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece a busca salva com os links de navegação.
 *
 * <p>Não há {@code self} para um {@code GET} de busca individual porque esse endpoint não
 * existe: a busca é lida na própria coleção — a do dono ou a vitrine pública. O link aponta
 * para o recurso de escrita, que é o que o cliente da API faz com ela em seguida.
 */
@Component
public class SavedSearchModelAssembler
        implements RepresentationModelAssembler<SavedSearchResponse,
        EntityModel<SavedSearchResponse>> {

    private static final String SAVED_SEARCHES_RELATION = "savedSearches";

    @Override
    @NonNull
    public EntityModel<SavedSearchResponse> toModel(@NonNull SavedSearchResponse savedSearch) {
        return EntityModel.of(savedSearch,
                linkTo(SavedSearchController.class).slash(savedSearch.id()).withSelfRel(),
                linkTo(SavedSearchController.class).withRel(SAVED_SEARCHES_RELATION));
    }
}
