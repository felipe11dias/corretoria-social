package com.corretoria.controller;

import com.corretoria.service.ListingFavoriteService;
import com.corretoria.vo.v1.response.ListingFavoriteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Favoritos do cliente.
 *
 * <p>{@code PUT} e {@code DELETE}, e não {@code POST}: o recurso é a relação entre quem chama
 * e o anúncio da rota — existe ou não existe. Marcar duas vezes deixa um favorito, e desmarcar
 * o que não estava marcado não é erro; um botão de alternar precisa dessa idempotência.
 *
 * <p>A rota mistura duas bases porque o recurso é o mesmo visto de dois lados: a marcação
 * pertence ao anúncio, a lista pertence a quem marcou.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Favoritos", description = "Anúncios marcados pelo cliente")
public class ListingFavoriteController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String CUSTOMER_ONLY = "hasRole('CUSTOMER')";

    private final ListingFavoriteService listingFavoriteService;

    public ListingFavoriteController(ListingFavoriteService listingFavoriteService) {
        this.listingFavoriteService = listingFavoriteService;
    }

    @PutMapping("/listings/{id}/favorite")
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Marca o anúncio como favorito",
            description = "Idempotente: marcar de novo devolve o mesmo favorito.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favoritado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Anúncio inexistente",
                    content = @Content)
    })
    public ListingFavoriteResponse favorite(@PathVariable Long id, Authentication authentication) {
        return listingFavoriteService.favorite(id, authentication);
    }

    @DeleteMapping("/listings/{id}/favorite")
    @PreAuthorize(CUSTOMER_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Desmarca o anúncio",
            description = "Idempotente: desmarcar o que não estava marcado também devolve 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desfavoritado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content)
    })
    public void unfavorite(@PathVariable Long id, Authentication authentication) {
        listingFavoriteService.unfavorite(id, authentication);
    }

    @GetMapping("/favorites")
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Lista os próprios favoritos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de favoritos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content)
    })
    public Page<ListingFavoriteResponse> findMine(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        return listingFavoriteService.findMine(authentication, pageable);
    }
}
