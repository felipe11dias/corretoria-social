package com.corretoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// O AuditorAware que preenche created_by/last_updated_by é resolvido por tipo:
// basta existir um único bean AuditorAware no contexto (SpringSecurityAuditorAware, Etapa 4).
@SpringBootApplication
@EnableJpaAuditing
public class CorretoriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorretoriaApplication.class, args);
    }
}
