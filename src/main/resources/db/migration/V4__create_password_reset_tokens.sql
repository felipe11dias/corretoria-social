-- Tokens de uso unico para redefinicao de senha.
--
-- Guarda o HASH do token, nunca o valor enviado ao usuario: quem le o banco nao consegue
-- redefinir a senha de ninguem.

CREATE TABLE password_reset_tokens (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_password_reset_tokens            PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_users      FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
