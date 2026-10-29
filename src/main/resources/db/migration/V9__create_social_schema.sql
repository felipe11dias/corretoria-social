-- Camada social: reputacao, favoritos, seguidores e mural do corretor.
--
-- Tres tabelas de vinculo puro (listing_favorites, realtor_follows, realtor_post_likes) tem
-- apenas created_at, e nao o pacote de auditoria completo: favoritar, seguir e curtir nao sao
-- operacoes que se "atualizam" -- sao criadas e desfeitas. Guardar quem alterou por ultimo um
-- vinculo que nunca muda seriam quatro colunas sem uso, mesmo raciocinio de user_roles.
--
-- Cada uma tem UNIQUE porque e isso que a interface promete: uma curtida por pessoa, um
-- favorito por anuncio, um vinculo por corretor -- e nao um contador que sobe a cada clique.

-- A reputacao so e util se for dificil de fabricar: avaliar exige ter havido negociacao entre
-- as partes (conferido no service) e vale uma por par, atualizavel. Sem o UNIQUE, um unico
-- cliente moveria a media do corretor repetindo a avaliacao.
CREATE TABLE reviews (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    realtor_id      BIGINT        NOT NULL,
    customer_id     BIGINT        NOT NULL,
    score           INTEGER       NOT NULL,
    comment         VARCHAR(2000),
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_reviews                      PRIMARY KEY (id),
    CONSTRAINT fk_reviews_realtors             FOREIGN KEY (realtor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customers            FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_reviews_realtor_id_customer_id UNIQUE (realtor_id, customer_id),
    CONSTRAINT ck_reviews_score_range          CHECK (score BETWEEN 1 AND 5),
    -- Ninguem avalia a si mesmo, nem por acidente de papel acumulado.
    CONSTRAINT ck_reviews_distinct_parties     CHECK (realtor_id <> customer_id)
);

CREATE INDEX idx_reviews_realtor_id ON reviews (realtor_id);

CREATE TABLE listing_favorites (
    id          BIGINT    GENERATED ALWAYS AS IDENTITY,
    customer_id BIGINT    NOT NULL,
    listing_id  BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT pk_listing_favorites                      PRIMARY KEY (id),
    CONSTRAINT fk_listing_favorites_users                FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_favorites_listings             FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT uk_listing_favorites_customer_id_listing_id UNIQUE (customer_id, listing_id)
);

CREATE INDEX idx_listing_favorites_customer_id ON listing_favorites (customer_id);

CREATE TABLE realtor_follows (
    id          BIGINT    GENERATED ALWAYS AS IDENTITY,
    customer_id BIGINT    NOT NULL,
    realtor_id  BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT pk_realtor_follows                       PRIMARY KEY (id),
    CONSTRAINT fk_realtor_follows_customers             FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_realtor_follows_realtors              FOREIGN KEY (realtor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_realtor_follows_customer_id_realtor_id UNIQUE (customer_id, realtor_id),
    CONSTRAINT ck_realtor_follows_distinct_parties      CHECK (customer_id <> realtor_id)
);

CREATE INDEX idx_realtor_follows_realtor_id ON realtor_follows (realtor_id);

-- O mural e o que separa a plataforma de um marketplace puro: o corretor tem presenca, e nao
-- apenas um estoque de anuncios.
CREATE TABLE realtor_posts (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    realtor_id      BIGINT        NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_realtor_posts       PRIMARY KEY (id),
    CONSTRAINT fk_realtor_posts_users FOREIGN KEY (realtor_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_realtor_posts_realtor_id ON realtor_posts (realtor_id);

-- Comentario e dependente do post, como proposal_messages e da proposta. Diferente do thread
-- de uma proposta, aqui a conversa e publica: qualquer usuario autenticado escreve.
CREATE TABLE realtor_post_comments (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    post_id         BIGINT        NOT NULL,
    author_id       BIGINT        NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_realtor_post_comments                PRIMARY KEY (id),
    CONSTRAINT fk_realtor_post_comments_realtor_posts  FOREIGN KEY (post_id) REFERENCES realtor_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_realtor_post_comments_users          FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_realtor_post_comments_post_id ON realtor_post_comments (post_id);

CREATE TABLE realtor_post_likes (
    id         BIGINT    GENERATED ALWAYS AS IDENTITY,
    post_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_realtor_post_likes                PRIMARY KEY (id),
    CONSTRAINT fk_realtor_post_likes_realtor_posts  FOREIGN KEY (post_id) REFERENCES realtor_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_realtor_post_likes_users          FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_realtor_post_likes_post_id_user_id UNIQUE (post_id, user_id)
);

CREATE INDEX idx_realtor_post_likes_post_id ON realtor_post_likes (post_id);
