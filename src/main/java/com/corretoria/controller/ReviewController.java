package com.corretoria.controller;

import com.corretoria.service.ReviewService;
import com.corretoria.vo.v1.request.ReviewRequest;
import com.corretoria.vo.v1.response.ReviewResponse;
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
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reputação do corretor.
 *
 * <p>Ler é público — reputação que só o dono enxerga não serve para escolher com quem
 * negociar. Avaliar exige ser cliente e <strong>ter negociado</strong> com o corretor.
 *
 * <p>O verbo é {@code PUT}, e não {@code POST}: cada cliente tem uma avaliação por corretor, e
 * avaliar de novo substitui a anterior — a operação é idempotente e o recurso é identificado
 * pelo par, não por um id novo a cada chamada.
 */
@RestController
@RequestMapping("/api/v1/realtors/{realtorId}/reviews")
@Tag(name = "Avaliações", description = "Reputação do corretor, condicionada a negociação real")
public class ReviewController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String CUSTOMER_ONLY = "hasRole('CUSTOMER')";

    /** Ver {@code NotificationController.UNREAD_COUNT_HEADER}: número não é item de página. */
    private static final String AVERAGE_SCORE_HEADER = "X-Corretoria-Average-Score";

    private final ReviewService reviewService;
    private final ReviewModelAssembler reviewModelAssembler;
    private final PagedResourcesAssembler<ReviewResponse> pagedResourcesAssembler;

    public ReviewController(
            ReviewService reviewService,
            ReviewModelAssembler reviewModelAssembler,
            PagedResourcesAssembler<ReviewResponse> pagedResourcesAssembler) {
        this.reviewService = reviewService;
        this.reviewModelAssembler = reviewModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @PutMapping
    @PreAuthorize(CUSTOMER_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Avalia o corretor",
            description = """
                    Exclusivo de `ROLE_CUSTOMER` que já tenha negociado com este corretor — \
                    basta a proposta ter existido, sem precisar ter sido aceita. Avaliar de \
                    novo substitui a nota anterior: é uma avaliação por par.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação registrada ou atualizada"),
            @ApiResponse(responseCode = "400", description = "Nota fora de 1 a 5",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Sem `ROLE_CUSTOMER` (`/problems/access-denied`) ou sem "
                            + "negociação prévia (`/problems/review-not-allowed`)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Corretor inexistente",
                    content = @Content)
    })
    public EntityModel<ReviewResponse> review(
            @PathVariable Long realtorId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        return reviewModelAssembler.toModel(
                reviewService.review(realtorId, request, authentication));
    }

    @GetMapping
    @Operation(
            summary = "Lista as avaliações do corretor",
            description = """
                    Endpoint público. O cabeçalho `X-Corretoria-Average-Score` traz a média de \
                    todas as avaliações, e não apenas das que couberam na página.""")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Página de avaliações"))
    public ResponseEntity<PagedModel<EntityModel<ReviewResponse>>> findByRealtor(
            @PathVariable Long realtorId,
            @PageableDefault(sort = "lastUpdatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReviewResponse> page = reviewService.findByRealtor(realtorId, pageable);

        return ResponseEntity.ok()
                .header(AVERAGE_SCORE_HEADER,
                        String.valueOf(reviewService.averageScoreOf(realtorId)))
                .body(pagedResourcesAssembler.toModel(page, reviewModelAssembler));
    }
}
