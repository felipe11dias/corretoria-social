package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.RealtorPostRepository;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por posse do mural.
 *
 * <p>Publicar no mural de alguém é falar em nome dele. Este bean responde à única pergunta que
 * importa antes de aceitar a publicação: o mural da rota é de quem está autenticado?
 */
@Component
public class RealtorPostSecurity {

    private final RealtorPostRepository realtorPostRepository;
    private final UserRepository userRepository;

    public RealtorPostSecurity(
            RealtorPostRepository realtorPostRepository, UserRepository userRepository) {
        this.realtorPostRepository = realtorPostRepository;
        this.userRepository = userRepository;
    }

    /** O mural pertence a quem está autenticado? */
    public boolean isOwnWall(Long realtorId, Authentication authentication) {
        if (realtorId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return userRepository.findByLogin(authentication.getName())
                .map(User::getId)
                .filter(realtorId::equals)
                .isPresent();
    }

    /** A publicação é de quem está autenticado? */
    public boolean isPostOwner(Long postId, Authentication authentication) {
        if (postId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return realtorPostRepository.findRealtorIdById(postId)
                .flatMap(realtorId -> userRepository.findByLogin(authentication.getName())
                        .map(User::getId)
                        .filter(realtorId::equals))
                .isPresent();
    }
}
