package com.corretoria.repository;

import com.corretoria.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso ao agregado raiz {@link User}.
 *
 * <p>Expõe apenas o que o serviço precisa: unicidade, autenticação e busca administrativa.
 * Endereços e tokens não têm repository próprio — pertencem a este agregado e são gerenciados
 * por cascade.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    Optional<User> findByLogin(String login);

    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
