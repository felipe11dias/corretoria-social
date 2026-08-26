package com.corretoria.exception;

/**
 * Avaliação sem negociação prévia — traduzida em {@code 403} com
 * {@code /problems/review-not-allowed}.
 *
 * <p>Não é falha de posse ({@code access-denied}) nem operação proibida em si
 * ({@code forbidden-operation}): a mesma pessoa pode avaliar este corretor amanhã, depois de
 * negociar com ele. O {@code type} próprio permite ao cliente da API dizer isso ao usuário em
 * vez de mostrar "acesso negado" para uma porta que se abre sozinha.
 */
public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException(String message) {
        super(message);
    }

    public static ReviewNotAllowedException requiresPreviousNegotiation() {
        return new ReviewNotAllowedException(
                "Avaliar um corretor exige ter negociado com ele: a nota vale porque é difícil "
                        + "de fabricar");
    }

    public static ReviewNotAllowedException cannotReviewYourself() {
        return new ReviewNotAllowedException("Ninguém avalia a si mesmo");
    }
}
