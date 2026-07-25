package com.corretoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base de auditoria de todas as entidades do projeto.
 *
 * <p>Registra, em toda escrita, <em>quando</em> e <em>quem</em>. O preenchimento é feito pelo
 * Spring Data JPA Auditing — um mecanismo declarativo único, em vez de {@code @PrePersist} e
 * {@code @PreUpdate} repetidos entidade a entidade.
 *
 * <p>Os campos de criação são imutáveis ({@code updatable = false}): uma escrita posterior não
 * reescreve quem criou o registro.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
}
