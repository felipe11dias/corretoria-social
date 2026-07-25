package com.corretoria.vo.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank(message = "O logradouro é obrigatório")
        @Size(max = 255, message = "O logradouro deve ter no máximo 255 caracteres")
        String street,

        @Size(max = 255, message = "O número deve ter no máximo 255 caracteres")
        String number,

        @Size(max = 255, message = "O complemento deve ter no máximo 255 caracteres")
        String complement,

        @Size(max = 255, message = "O bairro deve ter no máximo 255 caracteres")
        String neighborhood,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 255, message = "A cidade deve ter no máximo 255 caracteres")
        String city,

        @NotBlank(message = "A UF é obrigatória")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "A UF deve ter exatamente 2 letras")
        String state,

        // Aceita com ou sem máscara; o VO ZipCode normaliza para 8 dígitos no serviço.
        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "O CEP deve ter o formato 00000-000")
        String zipCode) {
}
