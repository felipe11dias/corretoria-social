package com.corretoria.vo.v1.response;

public record AddressResponse(
        Long id,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode) {
}
