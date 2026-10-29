package com.corretoria.controller;

import com.corretoria.service.ProposalService;
import com.corretoria.vo.v1.request.ProposalCreationRequest;
import com.corretoria.vo.v1.request.ProposalMessageCreationRequest;
import com.corretoria.vo.v1.request.ProposalStatusUpdateRequest;
import com.corretoria.vo.v1.response.ProposalMessageResponse;
import com.corretoria.vo.v1.response.ProposalResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da negociação.
 *
 * <p>Toda rota exige token, e as que tocam uma proposta específica exigem <strong>participar
 * dela</strong>: o thread é conversa particular entre o cliente e o corretor dono do anúncio.
 * Nem o administrador entra — pelo mesmo motivo de não haver escopo administrativo sobre
 * notificação.
 */
@RestController
@RequestMapping("/api/v1/proposals")
@Tag(name = "Propostas", description = "Negociação entre cliente e corretor")
public class ProposalController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String AUTHENTICATED = "isAuthenticated()";

    /**
     * Participante da negociação — os dois lados, e mais ninguém. Sem escopo administrativo,
     * como em {@code NotificationController}.
     */
    private static final String PARTICIPANT_ONLY =
            "@proposalSecurity.isParticipant(#id, authentication)";

    private final ProposalService proposalService;
    private final ProposalModelAssembler proposalModelAssembler;
    private final PagedResourcesAssembler<ProposalResponse> pagedResourcesAssembler;

    public ProposalController(
            ProposalService proposalService,
            ProposalModelAssembler proposalModelAssembler,
            PagedResourcesAssembler<ProposalResponse> pagedResourcesAssembler) {
        this.proposalService = proposalService;
        this.proposalModelAssembler = proposalModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @PostMapping
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Abre uma negociação",
            description = """
                    A direção é deduzida de quem está autenticado, não informada no corpo. \
                    Cliente propõe sobre um anúncio do catálogo; corretor propõe o próprio \
                    anúncio a partir de uma busca pública, e aí `savedSearchId` é obrigatório. \
                    Só anúncio `ACTIVE` aceita proposta.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criada, com cabeçalho Location"),
            @ApiResponse(responseCode = "400",
                    description = "Campos inválidos (`/problems/validation-error`) ou "
                            + "combinação inexistente (`/problems/invalid-proposal`)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Corretor propondo anúncio de outro corretor sem ser cliente "
                            + "(`/problems/forbidden-operation`)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anúncio ou busca inexistente",
                    content = @Content)
    })
    public ResponseEntity<EntityModel<ProposalResponse>> create(
            @Valid @RequestBody ProposalCreationRequest request, Authentication authentication) {

        EntityModel<ProposalResponse> model = proposalModelAssembler.toModel(
                proposalService.create(request, authentication));

        URI location = URI.create(model.getRequiredLink(IanaLinkRelations.SELF).getHref());
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Lista as próprias negociações",
            description = """
                    Uma lista só, com os dois papéis: as propostas em que o usuário é o \
                    cliente e as dos anúncios em que ele é o corretor.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de propostas"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content)
    })
    public PagedModel<EntityModel<ProposalResponse>> findMine(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<ProposalResponse> page = proposalService.findMine(authentication, pageable);
        return pagedResourcesAssembler.toModel(page, proposalModelAssembler);
    }

    @GetMapping("/{id}")
    @PreAuthorize(PARTICIPANT_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Abre uma negociação com o thread completo",
            description = "Exclusivo dos dois participantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta com as mensagens"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Negociação de terceiros (`/problems/access-denied`)",
                    content = @Content)
    })
    public EntityModel<ProposalResponse> findById(@PathVariable Long id) {
        return proposalModelAssembler.toModel(proposalService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(PARTICIPANT_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Aceita, recusa ou retira a proposta",
            description = """
                    Aceitar e recusar são da contraparte; retirar é de quem propôs. Só vale a \
                    partir de `PENDING`: os três desfechos são definitivos, e retomar é abrir \
                    uma proposta nova.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação alterada"),
            @ApiResponse(responseCode = "400",
                    description = "Proposta já decidida (`/problems/invalid-proposal`)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Negociação de terceiros, ou lado errado da mesa "
                            + "(`/problems/forbidden-operation`)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public EntityModel<ProposalResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProposalStatusUpdateRequest request,
            Authentication authentication) {

        return proposalModelAssembler.toModel(
                proposalService.changeStatus(id, request, authentication));
    }

    /** Contrapor é mandar mensagem sugerindo outro valor — não há estado de contraproposta. */
    @PostMapping("/{id}/messages")
    @PreAuthorize(PARTICIPANT_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Escreve no thread da proposta",
            description = """
                    Aberto aos dois participantes, inclusive depois do desfecho: combinar \
                    detalhes de uma proposta aceita é conversa legítima.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensagem publicada"),
            @ApiResponse(responseCode = "400", description = "Conteúdo vazio ou longo demais",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Negociação de terceiros",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public ProposalMessageResponse addMessage(
            @PathVariable Long id,
            @Valid @RequestBody ProposalMessageCreationRequest request,
            Authentication authentication) {

        return proposalService.addMessage(id, request, authentication);
    }
}
