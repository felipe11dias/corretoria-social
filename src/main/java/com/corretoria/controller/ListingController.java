package com.corretoria.controller;

import com.corretoria.service.ListingService;
import com.corretoria.vo.shared.PhotoContent;
import com.corretoria.vo.v1.request.ListingCreationRequest;
import com.corretoria.vo.v1.request.ListingSearchRequest;
import com.corretoria.vo.v1.request.ListingStatusUpdateRequest;
import com.corretoria.vo.v1.request.ListingUpdateRequest;
import com.corretoria.vo.v1.response.ListingPhotoResponse;
import com.corretoria.vo.v1.response.ListingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints do catálogo de anúncios.
 *
 * <p>Leitura é pública: qualquer visitante lista e abre anúncio sem token. Toda escrita exige
 * autenticação e posse — publicar exige {@code ROLE_REALTOR}; editar, pausar e mexer nas fotos
 * exige ser o dono do anúncio ou administrador.
 */
@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Anúncios", description = "Publicação e busca de anúncios de imóveis e automóveis")
public class ListingController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    /** Só corretor publica; cliente busca e negocia, nunca oferta. */
    private static final String REALTOR_ONLY = "hasRole('REALTOR')";

    /**
     * Dono do anúncio ou administrador. Sem o {@code isOwner}, qualquer corretor autenticado
     * editaria o anúncio de outro trocando o id da URL (IDOR).
     */
    private static final String OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @listingSecurity.isOwner(#id, authentication)";

    private final ListingService listingService;
    private final ListingModelAssembler listingModelAssembler;
    private final PagedResourcesAssembler<ListingResponse> pagedResourcesAssembler;

    public ListingController(
            ListingService listingService,
            ListingModelAssembler listingModelAssembler,
            PagedResourcesAssembler<ListingResponse> pagedResourcesAssembler) {
        this.listingService = listingService;
        this.listingModelAssembler = listingModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @PostMapping
    @PreAuthorize(REALTOR_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Publica um anúncio",
            description = """
                    Exclusivo de `ROLE_REALTOR`. O corretor dono vem do token, nunca do corpo. \
                    Informe `property` ou `vehicle` conforme o `type`. A publicação é \
                    imediata, sem fila de aprovação.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicado, com cabeçalho Location"),
            @ApiResponse(responseCode = "400",
                    description = "Campos inválidos (`/problems/validation-error`) ou "
                            + "combinação inexistente no domínio (`/problems/invalid-listing`)",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente ou inválido (`/problems/missing-token`)",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Usuário sem `ROLE_REALTOR` (`/problems/access-denied`)",
                    content = @Content)
    })
    public ResponseEntity<EntityModel<ListingResponse>> publish(
            @Valid @RequestBody ListingCreationRequest request, Authentication authentication) {

        EntityModel<ListingResponse> model =
                listingModelAssembler.toModel(listingService.publish(request, authentication));

        URI location = URI.create(model.getRequiredLink(IanaLinkRelations.SELF).getHref());
        return ResponseEntity.created(location).body(model);
    }

    /**
     * Busca do catálogo, sempre paginada.
     *
     * <p>Todo filtro é opcional e combinável: tipo, operação, localidade, faixa de preço,
     * atributos do imóvel ou do automóvel e raio a partir de uma coordenada.
     */
    @GetMapping
    @Operation(
            summary = "Busca anúncios, paginado",
            description = """
                    Endpoint público. Sem `status`, devolve apenas anúncios `ACTIVE`; outra \
                    situação exige ser o corretor dono (com `realtorId`) ou `ROLE_ADMIN`. \
                    A busca por raio exige `latitude`, `longitude` e `radiusKm` juntos. \
                    Aceita `page`, `size` e `sort` (padrão `createdAt,desc`).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de anúncios com links"),
            @ApiResponse(responseCode = "400",
                    description = "Critérios incoerentes, como raio sem coordenada "
                            + "(`/problems/invalid-field-format`)", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Situação diferente de `ACTIVE` sem posse "
                            + "(`/problems/forbidden-operation`)", content = @Content)
    })
    public PagedModel<EntityModel<ListingResponse>> search(
            @ModelAttribute ListingSearchRequest request,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<ListingResponse> page = listingService.search(request, pageable, authentication);
        return pagedResourcesAssembler.toModel(page, listingModelAssembler);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consulta um anúncio por id",
            description = "Endpoint público, sem token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anúncio encontrado"),
            @ApiResponse(responseCode = "404",
                    description = "Id inexistente (`/problems/resource-not-found`)",
                    content = @Content)
    })
    public EntityModel<ListingResponse> findById(@PathVariable Long id) {
        return listingModelAssembler.toModel(listingService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Atualiza o anúncio",
            description = """
                    O tipo do anúncio é imutável: o detalhamento enviado deve corresponder ao \
                    tipo já registrado. A situação tem endpoint próprio.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou detalhamento "
                    + "incompatível com o tipo", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Anúncio de outro corretor",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public EntityModel<ListingResponse> update(
            @PathVariable Long id, @Valid @RequestBody ListingUpdateRequest request) {

        return listingModelAssembler.toModel(listingService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(OWNER_OR_ADMIN)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Muda a situação do anúncio",
            description = """
                    `ACTIVE`, `PAUSED`, `REMOVED` ou `COMPLETED`. `COMPLETED` é sinalização \
                    do corretor de que o negócio saiu — não há registro de transação.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação alterada"),
            @ApiResponse(responseCode = "400", description = "Situação inválida",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Anúncio de outro corretor",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public EntityModel<ListingResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ListingStatusUpdateRequest request) {

        return listingModelAssembler.toModel(listingService.changeStatus(id, request));
    }

    /** A primeira foto enviada vira a capa do anúncio. */
    @PostMapping(path = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Envia uma foto do anúncio",
            description = """
                    `multipart/form-data` com a parte `photo`. Aceita `image/jpeg`, \
                    `image/png` e `image/webp`, até 5 MB e 20 fotos por anúncio. A primeira \
                    foto vira a capa.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Foto enviada"),
            @ApiResponse(responseCode = "400",
                    description = "Formato não suportado (`/problems/invalid-field-format`) ou "
                            + "limite de fotos atingido (`/problems/invalid-listing`)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Anúncio de outro corretor",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public ListingPhotoResponse addPhoto(
            @PathVariable Long id, @RequestPart("photo") MultipartFile photo) {

        return listingService.addPhoto(id, toPhotoContent(photo));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Remove uma foto do anúncio",
            description = "Remove também o objeto do storage. Excluir a capa promove a próxima foto.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Foto removida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Anúncio de outro corretor",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Anúncio ou foto inexistente",
                    content = @Content)
    })
    public void deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        listingService.deletePhoto(id, photoId);
    }

    /**
     * Converte a parte enviada no VO de conteúdo.
     *
     * <p>É aqui que o {@code multipart} para: da fronteira para dentro trafega
     * {@link PhotoContent}, e a camada de negócio não conhece {@code MultipartFile}.
     *
     * <p>Falha de leitura do fluxo é erro de servidor, não de requisição — daí o
     * {@code UncheckedIOException}, que o handler global traduz em {@code 500}.
     */
    private PhotoContent toPhotoContent(MultipartFile photo) {
        try {
            return new PhotoContent(photo.getBytes(), photo.getContentType());
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Falha ao ler o conteúdo da foto enviada", exception);
        }
    }
}
