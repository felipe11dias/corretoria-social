package com.corretoria.enums;

/**
 * Papéis de autorização da plataforma.
 *
 * <p>O prefixo {@code ROLE_} é exigido pelo Spring Security para montar as autoridades
 * ({@code GrantedAuthority}) — não é decoração. O papel do corretor é {@code ROLE_REALTOR};
 * {@code ROLE_OWNER} não existe no projeto.
 */
public enum RoleName {

    ROLE_ADMIN,
    ROLE_REALTOR,
    ROLE_CUSTOMER
}
