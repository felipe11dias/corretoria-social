package com.corretoria.exception;

/**
 * Busca salva incoerente — traduzida em {@code 400} com
 * {@code /problems/invalid-saved-search}.
 *
 * <p>Irmã de {@link InvalidListingException}, pelo mesmo motivo: cada campo é válido
 * isoladamente, mas a combinação não existe no domínio — critérios de imóvel numa busca de
 * automóvel, raio sem coordenada, faixa de preço invertida.
 */
public class InvalidSavedSearchException extends RuntimeException {

    public InvalidSavedSearchException(String message) {
        super(message);
    }

    public static InvalidSavedSearchException criteriaDoNotMatchType() {
        return new InvalidSavedSearchException(
                "Os critérios específicos devem ser os do tipo da busca: \"propertyCriteria\" "
                        + "para PROPERTY, \"vehicleCriteria\" para VEHICLE");
    }

    public static InvalidSavedSearchException radiusRequiresCoordinates() {
        return new InvalidSavedSearchException(
                "A busca por raio exige latitude e longitude");
    }

    public static InvalidSavedSearchException invertedPriceRange() {
        return new InvalidSavedSearchException(
                "O preço mínimo não pode ser maior que o preço máximo");
    }
}
