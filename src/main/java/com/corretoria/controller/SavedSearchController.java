package com.corretoria.controller;

import com.corretoria.service.SavedSearchService;
import com.corretoria.vo.v1.request.SavedSearchCreationRequest;
import com.corretoria.vo.v1.request.SavedSearchUpdateRequest;
import com.corretoria.vo.v1.response.SavedSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da busca salva.
 *
 * <p>Diferente do catálogo, aqui <strong>nada é público</strong>: mesmo a vitrine de procura
 * exige token. Uma busca revela o que a pessoa quer, onde e por quanto — expô-la a visitante
 * anônimo transformaria a plataforma em lista de leads aberta.
 */
@RestController
@RequestMapping("/api/v1/saved-searches")
@Tag(name = "Buscas salvas",
        description = "Alerta de anúncio novo e vitrine de anúncios de procura")
public class SavedSearchController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    /** Só cliente procura; o corretor oferta e consulta a vitrine. */
    private static final String CUSTOMER_ONLY = "hasRole('CUSTOMER')";

    private static final String AUTHENTICATED = "isAuthenticated()";

    /** Dono da busca ou administrador — ver {@code ListingController.OWNER_OR_ADMIN}. */
    private static final String OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @savedSearchSecurity.isOwner(#id, authentication)";

    private final SavedSearchService savedSearchService;
    private final SavedSearchModelAssembler savedSearchModelAssembler;
    private final PagedResourcesAssembler<SavedSearchResponse> pagedResourcesAssembler;

    public SavedSearchController(
            SavedSearchService savedSearchService,
            SavedSearchModelAssembler savedSearchModelAssembler,
            PagedResourcesAssembler<SavedSearchResponse> pagedResourcesAssembler) {
        this.savedSearchService = savedSearchService;
        this.savedSearchModelAssembler = savedSearchModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @PostMapping
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Cria uma busca salva",
            description = """
                    Exclusivo de `ROLE_CUSTOMER`. `PRIVATE` alerta apenas o dono quando um \
                    anúncio compatível é publicado; `PUBLIC` faz o mesmo e ainda expõe a \
                    procura na vitrine dos corretores. Os critérios específicos devem ser os \
                    do tipo da busca — ou nenhum.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criada, com cabeçalho Location"),
            @ApiResponse(responseCode = "400",
                    description = "Campos inválidos (`/problems/validation-error`) ou "
                            + "combinação inexistente (`/problems/invalid-saved-search`)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Usuário sem `ROLE_CUSTOMER` (`/problems/access-denied`)",
                    content = @Content)
    })
    public ResponseEntity<EntityModel<SavedSearchResponse>> create(
            @Valid @RequestBody SavedSearchCreationRequest request,
            Authentication authentication) {

        EntityModel<SavedSearchResponse> model = savedSearchModelAssembler.toModel(
                savedSearchService.create(request, authentication));

        URI location = URI.create(model.getRequiredLink(IanaLinkRelations.SELF).getHref());
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Lista as próprias buscas salvas",
            description = "Só as do usuário autenticado, públicas e privadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de buscas salvas"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content)
    })
    public PagedModel<EntityModel<SavedSearchResponse>> findMine(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<SavedSearchResponse> page = savedSearchService.findMine(authentication, pageable);
        return pagedResourcesAssembler.toModel(page, savedSearchModelAssembler);
    }

    /** Mercado reverso: é aqui que o corretor descobre quem está procurando o quê. */
    @GetMapping("/public")
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Vitrine de anúncios de procura",
            description = """
                    Buscas que os clientes tornaram públicas, e que continuam ativas. Exige \
                    token: uma procura revela o que a pessoa quer e onde.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de buscas públicas"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content)
    })
    public PagedModel<EntityModel<SavedSearchResponse>> findPublic(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<SavedSearchResponse> page = savedSearchService.findPublic(pageable);
        return pagedResourcesAssembler.toModel(page, savedSearchModelAssembler);
    }

    @PutMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Atualiza a busca salva",
            description = """
                    O tipo é imutável: os critérios enviados devem corresponder ao tipo já \
                    registrado. `active: false` pausa o alerta sem apagar os critérios.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizada"),
            @ApiResponse(responseCode = "400", description = "Campos ou combinação inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Busca de outro cliente",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public EntityModel<SavedSearchResponse> update(
            @PathVariable Long id, @Valid @RequestBody SavedSearchUpdateRequest request) {

        return savedSearchModelAssembler.toModel(savedSearchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Exclui a busca salva",
            description = "Remove também os critérios específicos, por cascade.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluída"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Busca de outro cliente",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public void delete(@PathVariable Long id) {
        savedSearchService.delete(id);
    }
}
