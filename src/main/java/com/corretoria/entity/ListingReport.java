package com.corretoria.entity;

import com.corretoria.enums.ReportReason;
import com.corretoria.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Denúncia de um anúncio.
 *
 * <p>Agregado raiz da moderação, e o contrapeso da publicação imediata: a plataforma confia no
 * corretor para publicar sem fila de aprovação, e confia na comunidade para apontar o que
 * estiver errado. Sem este contrapeso, a confiança do primeiro lado ficaria sem resposta.
 *
 * <p>Qualquer usuário autenticado abre uma denúncia; só {@code ROLE_ADMIN} a decide. Quem
 * decidiu fica em {@code lastUpdatedBy}, herdado de {@link Auditable} — um campo de "revisado
 * por" seria a mesma informação escrita duas vezes.
 */
@Getter
@Setter
@Entity
@Table(name = "listing_reports")
public class ListingReport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "listing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_reports_listings"))
    private Listing listing;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reporter_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_reports_users"))
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    /** Complemento em texto livre; a classificação é o {@code reason}, nunca este campo. */
    @Column(name = "detail", length = 2000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    /** Exigido pelo JPA e pelo MapStruct (ver {@link User#User()}). */
    public ListingReport() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListingReport report)) {
            return false;
        }
        return id != null && id.equals(report.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
