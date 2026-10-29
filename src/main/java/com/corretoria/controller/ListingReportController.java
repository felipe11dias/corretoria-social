package com.corretoria.controller;

import com.corretoria.vo.v1.request.ListingReportSearchRequest;
import com.corretoria.service.ListingReportService;
import com.corretoria.vo.v1.request.ListingReportCreationRequest;
import com.corretoria.vo.v1.request.ListingReportSearchRequest;
import com.corretoria.vo.v1.request.ListingReportStatusUpdateRequest;
import com.corretoria.vo.v1.response.ListingReportResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderação de anúncios.
 *
 * <p>Assimetria deliberada: <strong>abrir</strong> uma denúncia é de qualquer usuário
 * autenticado, porque o contrapeso da publicação imediata só funciona se denunciar for fácil;
 * <strong>ver a fila e decidir</strong> é exclusivo de {@code ROLE_ADMIN}, porque decisão de
 * moderação não é opinião de usuário.
 *
 * <p>Nem o denunciante acompanha a própria denúncia por aqui: a fila é ferramenta de trabalho
 * do moderador, e expô-la convidaria a discutir a decisão alheia em vez de abrir uma denúncia
 * nova quando o problema persistir.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Moderação", description = "Denúncia de anúncios e fila de decisão do administrador")
public class ListingReportController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String AUTHENTICATED = "isAuthenticated()";

    /** Decisão de moderação é do administrador: ver a fila também. */
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private final ListingReportService listingReportService;

    public ListingReportController(ListingReportService listingReportService) {
        this.listingReportService = listingReportService;
    }

    @PostMapping("/listings/{id}/reports")
    @PreAuthorize(AUTHENTICATED)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Denuncia um anúncio",
            description = """
                    Aberto a qualquer usuário autenticado. O denunciante vem do token, e \
                    denunciar o próprio anúncio responde 403 — para isso existe o endpoint de \
                    situação. Uma denúncia em aberto por pessoa e anúncio.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Denúncia registrada"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Denúncia do próprio anúncio "
                            + "(`/problems/forbidden-operation`)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anúncio inexistente",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Já existe denúncia sua em aberto para este anúncio "
                            + "(`/problems/duplicate-resource`)", content = @Content)
    })
    public ListingReportResponse report(
            @PathVariable Long id,
            @Valid @RequestBody ListingReportCreationRequest request,
            Authentication authentication) {

        return listingReportService.report(id, request, authentication);
    }

    @GetMapping("/reports")
    @PreAuthorize(ADMIN_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Fila de moderação",
            description = """
                    Exclusivo de `ROLE_ADMIN`. Sem `status`, devolve as denúncias `OPEN`, das \
                    mais antigas para as mais novas — quem esperou mais é atendido primeiro. \
                    As decididas continuam consultáveis com `status=REVIEWED` ou \
                    `status=DISMISSED`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de denúncias"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem `ROLE_ADMIN`",
                    content = @Content)
    })
    public Page<ListingReportResponse> findQueue(
            @ModelAttribute ListingReportSearchRequest request,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return listingReportService.findQueue(request, pageable);
    }

    @PatchMapping("/reports/{id}/status")
    @PreAuthorize(ADMIN_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Decide a denúncia",
            description = """
                    `REVIEWED` acolhe, `DISMISSED` considera improcedente — e a decisão é \
                    definitiva. Decidir **não** mexe no anúncio: retirá-lo do ar continua \
                    sendo uma ação explícita sobre ele, para que um clique na fila não apague \
                    o anúncio de alguém por engano.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Denúncia decidida"),
            @ApiResponse(responseCode = "400", description = "Situação ausente ou inválida",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Usuário sem `ROLE_ADMIN`, denúncia já decidida ou destino "
                            + "`OPEN`", content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public ListingReportResponse decide(
            @PathVariable Long id,
            @Valid @RequestBody ListingReportStatusUpdateRequest request) {

        return listingReportService.decide(id, request);
    }
}
