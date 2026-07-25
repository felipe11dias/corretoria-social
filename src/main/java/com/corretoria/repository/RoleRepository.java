package com.corretoria.repository;

import com.corretoria.entity.Role;
import com.corretoria.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Resolve o {@link RoleName} solicitado na requisição para a entidade {@link Role}
 * persistida, que é o que o vínculo em {@code user_roles} referencia.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
