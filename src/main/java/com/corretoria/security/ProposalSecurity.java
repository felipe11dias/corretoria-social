package com.corretoria.security;

import com.corretoria.entity.User;
import com.corretoria.repository.ProposalRepository;
import com.corretoria.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Autorização por participação na negociação.
 *
 * <p>Diferente dos outros beans de posse, aqui o recurso tem <strong>dois</strong> donos
 * legítimos: o cliente e o corretor dono do anúncio. Um thread de proposta é conversa
 * particular entre os dois — sem esta barreira, o id sequencial da URL abriria a negociação
 * alheia, com valores e mensagens.
 */
@Component
public class ProposalSecurity {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;

    public ProposalSecurity(ProposalRepository proposalRepository, UserRepository userRepository) {
        this.proposalRepository = proposalRepository;
        this.userRepository = userRepository;
    }

    public boolean isParticipant(Long proposalId, Authentication authentication) {
        if (proposalId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return userRepository.findByLogin(authentication.getName())
                .map(User::getId)
                .filter(userId -> proposalRepository.isParticipant(proposalId, userId))
                .isPresent();
    }
}
