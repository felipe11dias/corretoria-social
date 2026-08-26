package com.corretoria.repository;

import com.corretoria.entity.RealtorFollow;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a {@link RealtorFollow} — ver {@link ListingFavoriteRepository}. */
public interface RealtorFollowRepository extends JpaRepository<RealtorFollow, Long> {

    Page<RealtorFollow> findByCustomerId(Long customerId, Pageable pageable);

    Optional<RealtorFollow> findByCustomerIdAndRealtorId(Long customerId, Long realtorId);

    /** Quantos seguem o corretor: número que aparece no perfil dele. */
    long countByRealtorId(Long realtorId);
}
