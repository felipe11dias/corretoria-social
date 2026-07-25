package com.corretoria.controller;

import com.corretoria.service.UserService;
import com.corretoria.vo.v1.request.PasswordChangeRequest;
import com.corretoria.vo.v1.request.UserRegistrationRequest;
import com.corretoria.vo.v1.request.UserUpdateRequest;
import com.corretoria.vo.v1.response.UserResponse;
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
public class UserController {

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
    public ResponseEntity<EntityModel<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        EntityModel<UserResponse> model =
                userModelAssembler.toModel(userService.register(request));

        URI location = URI.create(model.getRequiredLink(IanaLinkRelations.SELF).getHref());
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
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
    public EntityModel<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {

        return userModelAssembler.toModel(userService.update(id, request));
    }

    /** Endpoint exclusivo da senha: atualizar o cadastro nunca troca credencial. */
    @PatchMapping("/{id}/password")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {

        userService.changePassword(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
