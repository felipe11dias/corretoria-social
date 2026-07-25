package com.corretoria.entity;

import com.corretoria.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Papel de autorização persistido.
 *
 * <p>Os papéis vivem em tabela própria — e não em coluna enum de {@code users} — para que um
 * usuário possa acumular papéis sem mudança de schema, e porque é essa a estrutura que o
 * Spring Security consome para montar as autoridades.
 */
@Getter
@Setter
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name"))
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private RoleName name;

    protected Role() {
        // exigido pelo JPA
    }

    public Role(RoleName name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Role role)) {
            return false;
        }
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
