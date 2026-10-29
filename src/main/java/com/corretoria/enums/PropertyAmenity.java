package com.corretoria.enums;

/**
 * Comodidades do imóvel.
 *
 * <p>Lista fechada de propósito: um modelo de atributo dinâmico (EAV) permitiria "piscina",
 * "Piscina" e "área de piscina" como três comodidades distintas, e nenhum filtro de busca
 * confiável sairia disso.
 */
public enum PropertyAmenity {

    POOL,
    BARBECUE_GRILL,
    GYM,
    ELEVATOR,
    PLAYGROUND,
    PARTY_ROOM,
    CONCIERGE,
    FURNISHED,
    BALCONY,
    PETS_ALLOWED
}
