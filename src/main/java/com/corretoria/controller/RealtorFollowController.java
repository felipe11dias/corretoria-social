package com.corretoria.controller;

import com.corretoria.service.RealtorFollowService;
import com.corretoria.vo.v1.response.RealtorFollowResponse;
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
 * Corretores acompanhados pelo cliente.
 *
 * <p>Mesma forma do favorito, e pelo mesmo motivo: o recurso é a relação entre quem chama e o
 * corretor da rota — existe ou não existe. Daí {@code PUT}/{@code DELETE} idempotentes, em vez
 * de {@code POST}.
 *
 * <p>O corretor é notificado apenas quando o vínculo nasce: um cliente indeciso não deve
 * encher a caixa dele.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Seguir corretor", description = "Vínculo de acompanhamento do cliente")
public class RealtorFollowController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String CUSTOMER_ONLY = "hasRole('CUSTOMER')";

    private final RealtorFollowService realtorFollowService;

    public RealtorFollowController(RealtorFollowService realtorFollowService) {
        this.realtorFollowService = realtorFollowService;
    }

    @PutMapping("/realtors/{id}/follow")
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Passa a acompanhar o corretor",
            description = "Idempotente: seguir de novo devolve o mesmo vínculo, sem notificar outra vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passou a acompanhar"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Corretor inexistente",
                    content = @Content)
    })
    public RealtorFollowResponse follow(@PathVariable Long id, Authentication authentication) {
        return realtorFollowService.follow(id, authentication);
    }

    @DeleteMapping("/realtors/{id}/follow")
    @PreAuthorize(CUSTOMER_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Deixa de acompanhar",
            description = "Idempotente: deixar de seguir quem não era seguido também devolve 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deixou de acompanhar"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content)
    })
    public void unfollow(@PathVariable Long id, Authentication authentication) {
        realtorFollowService.unfollow(id, authentication);
    }

    @GetMapping("/follows")
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Lista os corretores que acompanha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de corretores acompanhados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_CUSTOMER`",
                    content = @Content)
    })
    public Page<RealtorFollowResponse> findMine(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        return realtorFollowService.findMine(authentication, pageable);
    }
}
