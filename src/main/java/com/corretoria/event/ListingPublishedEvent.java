package com.corretoria.event;

/**
 * Um anúncio foi publicado.
 *
 * <p>Carrega apenas o identificador, e não a entidade: o consumidor roda em outra transação e
 * em outra thread, onde uma entidade viajaria desanexada e possivelmente já vencida. Com o id,
 * quem consome relê o estado atual.
 *
 * <p>É o que desacopla o {@code ListingService} do casamento com buscas salvas: publicar um
 * anúncio continua sendo publicar um anúncio, mesmo quando amanhã três outros interessados
 * quiserem reagir ao mesmo fato.
 *
 * @param listingId identificador do anúncio publicado
 */
public record ListingPublishedEvent(Long listingId) {
}
