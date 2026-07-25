-- Colunas de auditoria: quando e por quem cada registro foi criado e alterado.
--
-- As tabelas ja estao populadas (papeis na V1, usuarios na V2/V3), e created_at/
-- last_updated_at sao NOT NULL. Adicionar coluna NOT NULL a uma tabela com linhas so
-- funciona com DEFAULT; por isso cada ALTER abaixo segue tres passos:
--
--   1. ADD COLUMN ... NOT NULL DEFAULT CURRENT_TIMESTAMP  -> preenche as linhas existentes
--   2. UPDATE ... SET created_by = 'system'               -> marca a origem das linhas de seed
--   3. ALTER COLUMN ... DROP DEFAULT                      -> devolve a responsabilidade ao app
--
-- O passo 3 e o que importa a longo prazo: com DEFAULT no banco, uma falha do Spring Data
-- Auditing passaria despercebida, porque o PostgreSQL preencheria a data no lugar dele.
--
-- user_roles nao recebe auditoria: e tabela associativa pura, nao mapeada como entidade, e
-- nenhuma coluna dela seria preenchida pelo AuditingEntityListener.

ALTER TABLE users
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by      VARCHAR(255),
    ADD COLUMN last_updated_by VARCHAR(255);

UPDATE users SET created_by = 'system', last_updated_by = 'system';

ALTER TABLE users
    ALTER COLUMN created_at      DROP DEFAULT,
    ALTER COLUMN last_updated_at DROP DEFAULT;

ALTER TABLE roles
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by      VARCHAR(255),
    ADD COLUMN last_updated_by VARCHAR(255);

UPDATE roles SET created_by = 'system', last_updated_by = 'system';

ALTER TABLE roles
    ALTER COLUMN created_at      DROP DEFAULT,
    ALTER COLUMN last_updated_at DROP DEFAULT;

ALTER TABLE addresses
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by      VARCHAR(255),
    ADD COLUMN last_updated_by VARCHAR(255);

UPDATE addresses SET created_by = 'system', last_updated_by = 'system';

ALTER TABLE addresses
    ALTER COLUMN created_at      DROP DEFAULT,
    ALTER COLUMN last_updated_at DROP DEFAULT;

ALTER TABLE password_reset_tokens
    ADD COLUMN created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by      VARCHAR(255),
    ADD COLUMN last_updated_by VARCHAR(255);

ALTER TABLE password_reset_tokens
    ALTER COLUMN created_at      DROP DEFAULT,
    ALTER COLUMN last_updated_at DROP DEFAULT;
