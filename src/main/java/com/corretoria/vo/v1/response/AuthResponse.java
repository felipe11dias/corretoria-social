package com.corretoria.vo.v1.response;

/**
 * Token emitido no login.
 *
 * <p>Única resposta autorizada a carregar credencial: o JWT é justamente o que o cliente
 * precisa receber. Todas as demais são proibidas de expor senha, hash ou token.
 *
 * @param token     JWT assinado em HMAC-SHA256
 * @param type      esquema de autenticação a usar no cabeçalho {@code Authorization}
 * @param expiresIn validade do token em milissegundos
 */
public record AuthResponse(String token, String type, long expiresIn) {

    private static final String BEARER = "Bearer";

    public static AuthResponse bearer(String token, long expiresIn) {
        return new AuthResponse(token, BEARER, expiresIn);
    }
}
