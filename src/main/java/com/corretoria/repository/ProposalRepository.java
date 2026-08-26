package com.corretoria.repository;

import com.corretoria.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso ao agregado raiz {@link Proposal}.
 *
 * <p>{@code ProposalMessage} não tem repository próprio: pertence a este agregado e é
 * gerenciada por cascade.
 */
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    /**
     * Propostas em que o usuário participa, nos dois papéis.
     *
     * <p>Uma consulta só, e não uma por papel: para quem usa a API, "minhas propostas" é uma
     * lista única — o mesmo usuário pode ser cliente em uma negociação e corretor em outra, e
     * paginar duas listas separadas seria problema do cliente da API, não solução.
     */
    @Query("""
            SELECT p FROM Proposal p
            WHERE p.customer.id = :userId
               OR p.listing.realtor.id = :userId
            """)
    Page<Proposal> findByParticipantId(@Param("userId") Long userId, Pageable pageable);

    /**
     * O usuário participa desta negociação?
     *
     * <p>Predicado no banco em vez de carregar o agregado: a checagem roda a cada escrita,
     * fora de transação, e navegar até {@code listing.realtor} traria proxies desanexados —
     * mesma razão de {@code ListingRepository.findRealtorIdById}.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Proposal p
            WHERE p.id = :proposalId
              AND (p.customer.id = :userId OR p.listing.realtor.id = :userId)
            """)
    boolean isParticipant(@Param("proposalId") Long proposalId, @Param("userId") Long userId);
}
