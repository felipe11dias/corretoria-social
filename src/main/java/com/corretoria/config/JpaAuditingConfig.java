package com.corretoria.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita o preenchimento automático das colunas de auditoria.
 *
 * <p>Fica em classe própria, e não na classe de aplicação, de propósito: em
 * {@code CorretoriaApplication} a anotação é carregada por <em>qualquer</em> teste de fatia,
 * inclusive os de web, que não têm entidades no contexto — e o Spring falha com
 * {@code JPA metamodel must not be empty}. Isolada aqui, ela só entra quando o contexto de
 * persistência realmente existe.
 *
 * <p>O {@code AuditorAware} é resolvido por tipo: basta haver um único bean no contexto
 * ({@code SpringSecurityAuditorAware}).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
