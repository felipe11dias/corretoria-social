package com.corretoria.enums;

/**
 * Natureza do bem anunciado.
 *
 * <p>É o discriminador do anúncio: cada {@code Listing} tem exatamente um detalhamento —
 * {@code Property} ou {@code Vehicle} —, nunca os dois.
 */
public enum ListingType {

    PROPERTY,
    VEHICLE
}
