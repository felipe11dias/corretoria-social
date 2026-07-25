package com.corretoria.vo.shared;

import com.corretoria.util.ObjectUtils;
import com.corretoria.util.TextUtils;
import java.util.regex.Pattern;

/**
 * E-mail do usuário, sempre em forma canônica.
 *
 * <p><strong>Invariante:</strong> formato válido e texto normalizado para minúsculas, sem
 * espaços nas pontas.
 *
 * <p>A normalização não é cosmética — é o que torna a regra de e-mail único <em>correta</em>.
 * Sem ela, {@code Joao@x.com} e {@code joao@x.com} passariam pela verificação de unicidade
 * como endereços distintos e o mesmo e-mail seria cadastrado duas vezes.
 */
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");

    public Email {
        ObjectUtils.requireNonBlank(value, "O e-mail é obrigatório");
        value = TextUtils.normalize(value);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("E-mail inválido: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
