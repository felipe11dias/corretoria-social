package com.corretoria.repository;

import com.corretoria.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Valida o token de redefinição de senha pelo seu <strong>hash</strong>.
 *
 * <p>A busca é pelo hash porque é isso que o banco guarda — o valor enviado ao usuário nunca
 * é persistido.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
