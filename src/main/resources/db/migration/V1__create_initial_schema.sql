-- Schema inicial: identidade, papeis e enderecos.
-- Toda constraint tem nome explicito (pk_/fk_/uk_/ck_/idx_ + tabela + colunas), para que a
-- mensagem de erro do banco diga qual regra foi violada.

CREATE TABLE users (
    id       BIGINT       GENERATED ALWAYS AS IDENTITY,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    login    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_login UNIQUE (login)
);

-- A busca de usuarios pelo nome e uma operacao do administrador (Etapa 11).
CREATE INDEX idx_users_name ON users (name);

CREATE TABLE roles (
    id   BIGINT      GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_roles      PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- Resolve o N:M entre usuarios e papeis. ON DELETE CASCADE nos dois lados: remover um
-- usuario ou um papel nao pode deixar vinculo orfao.
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles       PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE addresses (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY,
    user_id      BIGINT       NOT NULL,
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(255),
    complement   VARCHAR(255),
    neighborhood VARCHAR(255),
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(2)   NOT NULL,
    zip_code     VARCHAR(9)   NOT NULL,
    CONSTRAINT pk_addresses                PRIMARY KEY (id),
    CONSTRAINT fk_addresses_users          FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_addresses_state_length   CHECK (char_length(state) = 2),
    CONSTRAINT ck_addresses_zip_code_digits CHECK (zip_code ~ '^[0-9]{8}$')
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- Dados de referencia: sem papel cadastrado nenhum usuario pode ser criado.
INSERT INTO roles (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_REALTOR'),
    ('ROLE_CUSTOMER');
