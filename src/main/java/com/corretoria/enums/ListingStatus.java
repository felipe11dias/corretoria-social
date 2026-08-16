package com.corretoria.enums;

/**
 * Situação de publicação do anúncio.
 *
 * <p>{@code COMPLETED} é sinalização manual do corretor de que o negócio saiu — não existe
 * registro de transação, comissão ou histórico de venda nesta fase.
 */
public enum ListingStatus {

    ACTIVE,
    PAUSED,
    REMOVED,
    COMPLETED
}
