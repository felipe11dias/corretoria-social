package com.corretoria.repository;

import com.corretoria.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso ao agregado raiz {@link Notification}. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Ver {@code ListingRepository.findRealtorIdById}: projeção para a checagem de posse. */
    @Query("SELECT n.recipient.id FROM Notification n WHERE n.id = :id")
    Optional<Long> findRecipientIdById(@Param("id") Long id);
}
