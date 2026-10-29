package com.corretoria.enums;

/**
 * Motivo de uma denúncia de anúncio.
 *
 * <p>Lista fechada, e não texto livre: é o que permite ao administrador priorizar a fila —
 * suspeita de fraude não espera na mesma ordem que uma foto desatualizada. O texto livre
 * continua existindo em {@code detail}, como complemento, nunca como classificação.
 */
public enum ReportReason {

    MISLEADING_INFO,
    PROHIBITED_ITEM,
    SUSPECTED_FRAUD,
    INAPPROPRIATE_CONTENT,
    OTHER
}
