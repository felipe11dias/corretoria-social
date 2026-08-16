package com.corretoria.enums;

/**
 * Operação oferecida no anúncio.
 *
 * <p>{@code RENT} só existe para imóvel: aluguel de veículo é operação de locadora, fora do
 * escopo do corretor individual. A coerência é imposta no serviço e no banco.
 */
public enum OperationType {

    SALE,
    RENT
}
