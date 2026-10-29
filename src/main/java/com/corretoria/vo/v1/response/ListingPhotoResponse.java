package com.corretoria.vo.v1.response;

/**
 * Foto de um anúncio.
 *
 * <p>Expõe a URL montada, e não a chave do objeto: a chave é detalhe do storage, e o cliente
 * da API precisa de um endereço que ele consiga abrir.
 */
public record ListingPhotoResponse(Long id, String url, Integer position, boolean cover) {
}
