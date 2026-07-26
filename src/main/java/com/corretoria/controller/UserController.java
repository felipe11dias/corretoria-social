package com.corretoria.controller;

import com.corretoria.service.UserService;
import com.corretoria.vo.v1.request.PasswordChangeRequest;
import com.corretoria.vo.v1.request.UserRegistrationRequest;
import com.corretoria.vo.v1.request.UserUpdateRequest;
import com.corretoria.vo.v1.response.UserResponse;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de usuário, versionados por path.
 *
 * <p>Não contém regra de negócio: valida o VO de entrada, delega ao serviço e monta a
 * resposta. A autorização é declarativa — nunca {@code if} sobre papel no corpo do método.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Cadastro e gestão de usuários")
public class UserController {

    /**
     * Referencia o esquema declarado em {@code OpenApiConfig} pelo nome, e não pela constante,
     * para não criar dependência da apresentação sobre o pacote de configuração.
     */
    private static final String BEARER_AUTH = "bearerAuth";

    /**
     * Dono do recurso ou administrador. Sem o {@code isSelf}, qualquer token válido leria ou
     * alteraria qualquer usuário trocando o id da URL (IDOR).
     */
    private static final String OWNER_OR_ADMIN =
            "hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)";

    /** Consultas de coleção são da gestão de usuários, responsabilidade do administrador. */
    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private final UserService userService;
    private final UserModelAssembler userModelAssembler;
    private final PagedResourcesAssembler<UserResponse> pagedResourcesAssembler;

    public UserController(
            UserService userService,
            UserModelAssembler userModelAssembler,
            PagedResourcesAssembler<UserResponse> pagedResourcesAssembler) {
        this.userService = userService;
        this.userModelAssembler = userModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    /** Autocadastro de corretor ou cliente. Público — o serviço recusa {@code ROLE_ADMIN}. */
    @PostMapping
    @Operation(
            summary = "Autocadastro de corretor ou cliente",
            description = """
                    Endpoint público, sem token. Solicitar `ROLE_ADMIN` responde 403: papel \
                    privilegiado nasce apenas por seed ou por fluxo autenticado de \
                    administrador.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado, com cabeçalho Location"),
            @ApiResponse(responseCode = "400",
                    description = "Campos inválidos (`/problems/validation-error`) ou "
                            + "invariante de VO violada (`/problems/invalid-field-format`)",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Papel privilegiado solicitado "
                            + "(`/problems/forbidden-operation`)",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "E-mail ou login já cadastrado "
                            + "(`/problems/duplicate-resource`)",
                    content = @Content)
    })
    public ResponseEntity<EntityModel<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        EntityModel<UserResponse> model =
                userModelAssembler.toModel(userService.register(request));

        URI location = URI.create(model.getRequiredLink(IanaLinkRelations.SELF).getHref());
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Consulta um usuário por id",
            description = "Exige ser o dono do recurso ou `ROLE_ADMIN`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente, expirado ou inválido "
                            + "(`/problems/missing-token`)", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Recurso de outro usuário (`/problems/access-denied`)",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Id inexistente (`/problems/resource-not-found`)",
                    content = @Content)
    })
    public EntityModel<UserResponse> findById(@PathVariable Long id) {
        return userModelAssembler.toModel(userService.findById(id));
    }

    /**
     * Lista ou busca por nome, sempre paginado.
     *
     * <p>O mesmo endpoint atende aos dois casos: {@code name} ausente lista todos, presente
     * filtra por trecho do nome sem diferenciar maiúsculas de minúsculas.
     */
    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Lista ou busca usuários, paginado",
            description = """
                    Exclusivo de `ROLE_ADMIN`. Sem `name`, lista todos; com `name`, filtra por \
                    trecho do nome sem diferenciar maiúsculas. Aceita `page`, `size` e `sort` \
                    (padrão `name`).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários com links"),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente ou inválido (`/problems/missing-token`)",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Usuário sem `ROLE_ADMIN` (`/problems/access-denied`)",
                    content = @Content)
    })
    public PagedModel<EntityModel<UserResponse>> findAll(
            @RequestParam(required = false) String name,
            @PageableDefault(sort = "name") Pageable pageable) {

        Page<UserResponse> page = StringUtils.hasText(name)
                ? userService.findByName(name, pageable)
                : userService.findAll(pageable);

        return pagedResourcesAssembler.toModel(page, userModelAssembler);
    }

    @PutMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Atualiza os dados cadastrais",
            description = "Não altera senha nem papéis. Dono do recurso ou `ROLE_ADMIN`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Cadastro de outro usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "E-mail ou login já usado por outro usuário",
                    content = @Content)
    })
    public EntityModel<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {

        return userModelAssembler.toModel(userService.update(id, request));
    }

    /** Endpoint exclusivo da senha: atualizar o cadastro nunca troca credencial. */
    @PatchMapping("/{id}/password")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Troca a senha",
            description = """
                    Exige a senha atual mesmo com o usuário já autenticado: um token vazado \
                    não deve bastar para tomar a conta.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "400",
                    description = "Senha atual incorreta ou confirmação divergente "
                            + "(`/problems/invalid-password`)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Senha de outro usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente",
                    content = @Content)
    })
    public void changePassword(
            @PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {

        userService.changePassword(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = BEARER_AUTH)
    @Operation(
            summary = "Exclui o usuário",
            description = "Remove também endereços, tokens e vínculos de papel, por cascade.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Cadastro de outro usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Id inexistente",
                    content = @Content)
    })
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
