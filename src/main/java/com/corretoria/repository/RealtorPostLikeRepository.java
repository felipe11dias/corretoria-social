package com.corretoria.repository;

import com.corretoria.entity.RealtorPostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a {@link RealtorPostLike} — vínculo puro, consultado por contagem. */
public interface RealtorPostLikeRepository extends JpaRepository<RealtorPostLike, Long> {

    Optional<RealtorPostLike> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);
}
