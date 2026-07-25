package com.corretoria.mapper;

import com.corretoria.entity.Address;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.vo.v1.request.UserRegistrationRequest;
import com.corretoria.vo.v1.response.RoleResponse;
import com.corretoria.vo.v1.response.UserResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Tradução entre o agregado {@link User} e os VOs de contrato.
 *
 * <p>O mapeador não resolve senha nem papéis: a senha é o hash BCrypt aplicado pelo serviço e
 * os papéis exigem consulta ao {@code RoleRepository}. Deixar esses dois de fora é o que
 * impede a senha em texto plano de chegar à entidade.
 */
@Mapper(uses = AddressMapper.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "passwordResetTokens", ignore = true)
    User toEntity(UserRegistrationRequest request);

    UserResponse toResponse(User user);

    RoleResponse toRoleResponse(Role role);

    /**
     * Fecha a referência de volta dos endereços.
     *
     * <p>Sem isto, {@code addresses.user_id} sairia nulo e o {@code INSERT} violaria a
     * restrição {@code NOT NULL} da coluna.
     */
    @AfterMapping
    default void linkAddressesToUser(@MappingTarget User user) {
        for (Address address : user.getAddresses()) {
            address.setUser(user);
        }
    }
}
