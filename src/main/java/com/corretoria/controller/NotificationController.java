package com.corretoria.controller;

import com.corretoria.service.NotificationService;
import com.corretoria.vo.v1.response.NotificationResponse;
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
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de notificação.
 *
 * <p>Notificação é correspondência: cada um lê e marca como lida apenas a sua. Não há listagem
 * administrativa — nem o administrador tem por que ler o aviso alheio.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificações", description = "Avisos in-app do usuário autenticado")
public class NotificationController {

    /** Ver {@code UserController.BEARER_AUTH}. */
    private static final String BEARER_AUTH = "bearerAuth";

    private static final String AUTHENTICATED = "isAuthenticated()";

    /**
     * Contador de não lidas no cabeçalho, e não no corpo: o corpo é a página de notificações,
     * e o total pendente não é um item dela. Cabeçalho customizado segue o padrão
     * {@code X-Corretoria-<Assunto>}.
     */
    private static final String UNREAD_COUNT_HEADER = "X-Corretoria-Unread-Count";

    /**
     * Sem escopo administrativo aqui, de propósito: correspondência não tem supervisor. É a
     * razão de não reusar o {@code OWNER_OR_ADMIN} dos outros controllers.
     */
    private static final String RECIPIENT_ONLY =
            "@notificationSecurity.isRecipient(#id, authentication)";

    private final NotificationService notificationService;
    private final NotificationModelAssembler notificationModelAssembler;
    private final PagedResourcesAssembler<NotificationResponse> pagedResourcesAssembler;

    public NotificationController(
            NotificationService notificationService,
            NotificationModelAssembler notificationModelAssembler,
            PagedResourcesAssembler<NotificationResponse> pagedResourcesAssembler) {
        this.notificationService = notificationService;
        this.notificationModelAssembler = notificationModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Lista as próprias notificações",
            description = """
                    Paginado, das mais recentes para as mais antigas. O cabeçalho \
                    `X-Corretoria-Unread-Count` traz quantas ainda não foram lidas.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notificações"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content)
    })
    public ResponseEntity<PagedModel<EntityModel<NotificationResponse>>> findMine(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<NotificationResponse> page = notificationService.findMine(authentication, pageable);

        return ResponseEntity.ok()
                .header(UNREAD_COUNT_HEADER,
                        String.valueOf(notificationService.countUnread(authentication)))
                .body(pagedResourcesAssembler.toModel(page, notificationModelAssembler));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize(RECIPIENT_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Marca a notificação como lida",
            description = "Exclusivo do destinatário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marcada como lida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Notificação de outro usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente", content = @Content)
    })
    public EntityModel<NotificationResponse> markAsRead(@PathVariable Long id) {
        return notificationModelAssembler.toModel(notificationService.markAsRead(id));
    }
}
