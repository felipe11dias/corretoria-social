package com.corretoria.controller;

import com.corretoria.service.AuthenticationService;
import com.corretoria.vo.v1.request.LoginRequest;
import com.corretoria.vo.v1.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Emissão de token JWT")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autentica e emite o token JWT",
            description = """
                    Endpoint público. Credencial inválida responde 401 com a mesma mensagem \
                    tanto para login inexistente quanto para senha errada — distinguir os \
                    casos permitiria enumerar contas válidas.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "400",
                    description = "Campos obrigatórios ausentes (`/problems/validation-error`)",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Credenciais inválidas (`/problems/authentication-failed`)",
                    content = @Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}
