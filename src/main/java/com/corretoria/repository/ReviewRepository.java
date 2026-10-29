package com.corretoria.repository;

import com.corretoria.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso ao agregado raiz {@link Review}. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRealtorId(Long realtorId, Pageable pageable);

    /**
     * Avaliação existente do par, que é o que torna a operação um <em>upsert</em>.
     *
     * <p>Uma por par: avaliar de novo atualiza a nota, e não acumula outra.
     */
    Optional<Review> findByRealtorIdAndCustomerId(Long realtorId, Long customerId);

    /**
     * Média das notas do corretor.
     *
     * <p>Calculada no banco, e não somando as notas da página em memória: a média é de todas as
     * avaliações, não das vinte que couberam na primeira página.
     */
    @Query("SELECT avg(r.score) FROM Review r WHERE r.realtor.id = :realtorId")
    Optional<Double> findAverageScoreByRealtorId(@Param("realtorId") Long realtorId);
}
