package com.corretoria.repository;

import com.corretoria.entity.ListingFavorite;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link ListingFavorite}.
 *
 * <p>Vínculo puro, mas com repository próprio: "meus favoritos" é uma lista que o cliente abre,
 * e não um detalhe interno de outro agregado.
 */
public interface ListingFavoriteRepository extends JpaRepository<ListingFavorite, Long> {

    Page<ListingFavorite> findByCustomerId(Long customerId, Pageable pageable);

    /** O favorito existente do par — a marcação é idempotente e reaproveita este registro. */
    Optional<ListingFavorite> findByCustomerIdAndListingId(Long customerId, Long listingId);
}
