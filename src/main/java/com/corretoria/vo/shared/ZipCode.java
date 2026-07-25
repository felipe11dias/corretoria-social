package com.corretoria.vo.shared;

import com.corretoria.util.ObjectUtils;
import com.corretoria.util.TextUtils;

/**
 * CEP brasileiro.
 *
 * <p><strong>Invariante:</strong> exatamente 8 dígitos, guardados sem máscara.
 *
 * <p>Aceita {@code "60000-000"} ou {@code "60000000"} na entrada e persiste sempre a mesma
 * forma — a máscara é apresentação, e sai sob demanda em {@link #formatted()}. É a mesma
 * invariante imposta no banco pela constraint {@code ck_addresses_zip_code_digits}.
 */
public record ZipCode(String value) {

    private static final int LENGTH = 8;
    private static final int PREFIX_LENGTH = 5;

    public ZipCode {
        ObjectUtils.requireNonBlank(value, "O CEP é obrigatório");
        String digits = TextUtils.digitsOnly(value);
        if (digits.length() != LENGTH) {
            throw new IllegalArgumentException("CEP inválido: " + value);
        }
        value = digits;
    }

    /** Devolve o CEP no formato {@code 00000-000}. */
    public String formatted() {
        return value.substring(0, PREFIX_LENGTH) + "-" + value.substring(PREFIX_LENGTH);
    }

    @Override
    public String toString() {
        return value;
    }
}
