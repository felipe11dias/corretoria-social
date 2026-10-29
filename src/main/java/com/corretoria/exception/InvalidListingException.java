package com.corretoria.exception;

/**
 * Anúncio incoerente — traduzida em {@code 400} com {@code /problems/invalid-listing}.
 *
 * <p>Distinta de {@code /problems/validation-error}: ali cada campo é inválido isoladamente
 * (obrigatório ausente, tamanho estourado), e a Bean Validation resolve. Aqui cada campo é
 * válido por si, mas a <em>combinação</em> não existe no domínio — anúncio de veículo com
 * detalhamento de imóvel, aluguel de automóvel. É a distinção que o {@code type} do RFC 7807
 * permite fazer sem inventar outro status.
 */
public class InvalidListingException extends RuntimeException {

    public InvalidListingException(String message) {
        super(message);
    }

    public static InvalidListingException propertyDetailRequired() {
        return new InvalidListingException(
                "Anúncio do tipo PROPERTY exige o detalhamento de imóvel em \"property\"");
    }

    public static InvalidListingException vehicleDetailRequired() {
        return new InvalidListingException(
                "Anúncio do tipo VEHICLE exige o detalhamento de veículo em \"vehicle\"");
    }

    public static InvalidListingException detailDoesNotMatchType() {
        return new InvalidListingException(
                "Um anúncio tem um único detalhamento: informe \"property\" ou \"vehicle\", "
                        + "conforme o tipo, nunca os dois");
    }

    public static InvalidListingException vehicleCannotBeRented() {
        return new InvalidListingException(
                "Anúncio de veículo só admite a operação SALE: aluguel de automóvel é "
                        + "operação de locadora");
    }

    public static InvalidListingException photoLimitReached(int limit) {
        return new InvalidListingException(
                "O anúncio já tem o número máximo de fotos: " + limit);
    }
}
