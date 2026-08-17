package com.corretoria.enums;

/**
 * Alcance de uma busca salva.
 *
 * <p>É o campo que faz uma única entidade servir a dois usos: {@code PRIVATE} é o alerta
 * silencioso do cliente, {@code PUBLIC} é o "anúncio de procura" que os corretores enxergam.
 * Duas entidades separadas duplicariam critérios e matching idênticos.
 */
public enum SearchVisibility {

    PRIVATE,
    PUBLIC
}
