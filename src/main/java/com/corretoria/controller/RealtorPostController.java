package com.corretoria.controller;

import com.corretoria.service.RealtorPostService;
import com.corretoria.vo.v1.request.RealtorPostCommentCreationRequest;
import com.corretoria.vo.v1.request.RealtorPostCreationRequest;
import com.corretoria.vo.v1.response.RealtorPostCommentResponse;
import com.corretoria.vo.v1.response.RealtorPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mural do corretor.
 *
 * <p>Ler é público: o mural é vitrine de presença, e exigir token para lê-lo esvaziaria o
 * propósito. Publicar é do dono do mural; comentar e curtir são de qualquer usuário
 * autenticado — inclusive de outro corretor.
 *
 * <p>A publicação vive sob o corretor ({@code /realtors/{realtorId}/posts}), porque é dele; o
 * comentário e a curtida vivem sob a publicação ({@code /posts/{postId}/...}), porque quem
 * interage já sabe em que post está e não deveria repetir o dono na URL.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mural", description = "Publicações do corretor, com comentários e curtidas")
public class RealtorPostController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String AUTHENTICATED = "isAuthenticated()";

    /** Publicar no mural de alguém é falar em nome dele. */
    private static final String OWN_WALL = "@realtorPostSecurity.isOwnWall(#realtorId, authentication)";

    private final RealtorPostService realtorPostService;

    public RealtorPostController(RealtorPostService realtorPostService) {
        this.realtorPostService = realtorPostService;
    }

    @PostMapping("/realtors/{realtorId}/posts")
    @PreAuthorize(OWN_WALL)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Publica no próprio mural",
            description = "Exclusivo do corretor dono do mural.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicado"),
            @ApiResponse(responseCode = "400", description = "Conteúdo vazio ou longo demais",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Mural de outro corretor",
                    content = @Content)
    })
    public RealtorPostResponse publish(
            @PathVariable Long realtorId,
            @Valid @RequestBody RealtorPostCreationRequest request,
            Authentication authentication) {

        return realtorPostService.publish(request, authentication);
    }

    @GetMapping("/realtors/{realtorId}/posts")
    @Operation(summary = "Lista o mural do corretor", description = "Endpoint público.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Página de publicações"))
    public Page<RealtorPostResponse> findByRealtor(
            @PathVariable Long realtorId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return realtorPostService.findByRealtor(realtorId, pageable);
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize(AUTHENTICATED)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Comenta uma publicação",
            description = "Aberto a qualquer usuário autenticado, cliente ou corretor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comentário publicado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Publicação inexistente",
                    content = @Content)
    })
    public RealtorPostCommentResponse comment(
            @PathVariable Long postId,
            @Valid @RequestBody RealtorPostCommentCreationRequest request,
            Authentication authentication) {

        return realtorPostService.comment(postId, request, authentication);
    }

    @PutMapping("/posts/{postId}/like")
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Curte a publicação",
            description = "Idempotente: curtir de novo não vira uma segunda curtida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curtida registrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Publicação inexistente",
                    content = @Content)
    })
    public RealtorPostResponse like(@PathVariable Long postId, Authentication authentication) {
        return realtorPostService.like(postId, authentication);
    }

    @DeleteMapping("/posts/{postId}/like")
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(summary = "Retira a curtida", description = "Idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curtida retirada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Publicação inexistente",
                    content = @Content)
    })
    public RealtorPostResponse unlike(@PathVariable Long postId, Authentication authentication) {
        return realtorPostService.unlike(postId, authentication);
    }
}
