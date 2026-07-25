package com.corretoria.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Identidade e credenciais de um usuário da plataforma — corretor, cliente ou administrador.
 *
 * <p>A senha existe aqui apenas como <strong>hash BCrypt</strong>; o texto plano nunca é
 * persistido nem trafega para fora do serviço.
 */
@Getter
@Setter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_login", columnNames = "login")
        })
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    /** Sempre em minúsculas: a normalização é o que torna a regra de e-mail único correta. */
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "password", nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_users")),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_roles")))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    protected User() {
        // exigido pelo JPA
    }

    public User(String name, String email, String login, String password) {
        this.name = name;
        this.email = email;
        this.login = login;
        this.password = password;
    }

    /**
     * Substitui os endereços mantendo a referência de volta consistente.
     *
     * <p>Sem reaproveitar a mesma lista gerenciada, o {@code orphanRemoval} do Hibernate não
     * enxerga a remoção dos endereços antigos.
     */
    public void replaceAddresses(List<Address> newAddresses) {
        addresses.clear();
        newAddresses.forEach(this::addAddress);
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
