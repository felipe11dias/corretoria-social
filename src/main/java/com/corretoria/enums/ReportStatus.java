package com.corretoria.enums;

/**
 * Situação de uma denúncia na fila de moderação.
 *
 * <p>{@code OPEN} é o que espera decisão; {@code REVIEWED} é a denúncia acolhida e
 * {@code DISMISSED} a improcedente. Os dois desfechos são registrados em vez de a linha ser
 * apagada: saber que um anúncio já foi denunciado e liberado é o que evita reabrir a mesma
 * discussão a cada nova denúncia.
 *
 * <p>Quem decidiu fica em {@code lastUpdatedBy}, herdado de {@code Auditable} — não há campo
 * separado de revisor.
 */
public enum ReportStatus {

    OPEN,
    REVIEWED,
    DISMISSED
}
