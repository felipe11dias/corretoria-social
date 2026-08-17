package com.corretoria.enums;

/**
 * Motivo de uma notificação.
 *
 * <p>Os tipos além de {@code SEARCH_MATCH} pertencem aos clusters seguintes da Fase 2
 * (negociação, social e moderação) e já entram no enum para que a tabela nasça com o domínio
 * completo — o {@code CHECK} do banco não precisará ser reescrito a cada cluster.
 */
public enum NotificationType {

    SEARCH_MATCH,
    NEW_PROPOSAL,
    PROPOSAL_STATUS_CHANGED,
    NEW_PROPOSAL_MESSAGE,
    NEW_REVIEW,
    NEW_FOLLOWER,
    LISTING_REPORTED
}
