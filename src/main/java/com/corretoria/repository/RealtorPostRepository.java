package com.corretoria.repository;

import com.corretoria.entity.RealtorPost;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso ao agregado raiz {@link RealtorPost}.
 *
 * <p>{@code RealtorPostComment} não tem repository próprio: pertence a este agregado e é
 * gerenciado por cascade. A curtida tem, por ser vínculo consultado por contagem.
 */
public interface RealtorPostRepository extends JpaRepository<RealtorPost, Long> {

    Page<RealtorPost> findByRealtorId(Long realtorId, Pageable pageable);

    /** Ver {@code ListingRepository.findRealtorIdById}: projeção para a checagem de posse. */
    @Query("SELECT p.realtor.id FROM RealtorPost p WHERE p.id = :postId")
    Optional<Long> findRealtorIdById(@Param("postId") Long postId);
}
