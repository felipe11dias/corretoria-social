package com.corretoria.exception;

/**
 * Operação não permitida pela regra de negócio — traduzida em {@code 403} com
 * {@code /problems/forbidden-operation}.
 *
 * <p>Distinta de {@code AccessDeniedException}, que significa "este recurso não é seu". Aqui
 * a operação é proibida <em>em si</em>: o caso do projeto é o autocadastro público
 * solicitando {@code ROLE_ADMIN}, tentativa de escalonamento de privilégio. Dois erros com o
 * mesmo {@code 403} e {@code type} diferente — que é exatamente o que o RFC 7807 resolve.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public static ForbiddenOperationException selfRegistrationCannotGrantAdmin() {
        return new ForbiddenOperationException(
                "O autocadastro público não concede o papel ROLE_ADMIN");
    }

    public static ForbiddenOperationException realtorRoleRequired() {
        return new ForbiddenOperationException(
                "Apenas usuários com o papel ROLE_REALTOR publicam anúncios");
    }

    public static ForbiddenOperationException customerRoleRequired() {
        return new ForbiddenOperationException(
                "Apenas usuários com o papel ROLE_CUSTOMER criam buscas salvas");
    }

    /**
     * Consulta de anúncio fora do ar por quem não é o dono.
     *
     * <p>Anúncio pausado, removido ou concluído saiu do catálogo por decisão do corretor;
     * deixar qualquer visitante listá-lo pelo filtro de situação anularia essa decisão.
     */
    public static ForbiddenOperationException nonActiveSearchRequiresOwnership() {
        return new ForbiddenOperationException(
                "Buscar anúncios com situação diferente de ACTIVE exige ser o corretor dono "
                        + "(filtro realtorId) ou ter o papel ROLE_ADMIN");
    }
}
