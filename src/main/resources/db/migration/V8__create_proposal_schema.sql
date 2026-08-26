-- Negociacao: proposta sobre um anuncio e o thread de mensagens dela.
--
-- A proposta e bidirecional. O cliente propoe sobre um anuncio que viu; o corretor propoe a
-- partir de uma busca publica de um cliente, e nesse caso origin_saved_search_id guarda de
-- onde veio a abordagem -- sem esse rastro o cliente receberia uma proposta sem saber por que
-- aquele corretor o procurou.
--
-- customer_id e sempre a contraparte cliente, independente de quem iniciou; o corretor da
-- negociacao e o dono do anuncio. Guardar os papeis por funcao, e nao por "quem propos", e o
-- que mantem a consulta dos dois lados simples.

CREATE TABLE proposals (
    id                     BIGINT        GENERATED ALWAYS AS IDENTITY,
    listing_id             BIGINT        NOT NULL,
    customer_id            BIGINT        NOT NULL,
    initiator              VARCHAR(20)   NOT NULL,
    origin_saved_search_id BIGINT,
    amount                 NUMERIC(15,2) NOT NULL,
    status                 VARCHAR(20)   NOT NULL,
    created_at             TIMESTAMP     NOT NULL,
    last_updated_at        TIMESTAMP     NOT NULL,
    created_by             VARCHAR(255),
    last_updated_by        VARCHAR(255),
    CONSTRAINT pk_proposals                PRIMARY KEY (id),
    CONSTRAINT fk_proposals_listings       FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposals_users          FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    -- A busca pode ser apagada sem levar junto a negociacao que ela originou: o rastro some,
    -- a proposta permanece.
    CONSTRAINT fk_proposals_saved_searches FOREIGN KEY (origin_saved_search_id) REFERENCES saved_searches (id) ON DELETE SET NULL,
    CONSTRAINT ck_proposals_initiator      CHECK (initiator IN ('CUSTOMER', 'REALTOR')),
    CONSTRAINT ck_proposals_status         CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_proposals_amount_positive CHECK (amount > 0),
    -- Rastro de origem so existe quando foi o corretor que tomou a iniciativa.
    CONSTRAINT ck_proposals_origin_initiator CHECK (origin_saved_search_id IS NULL OR initiator = 'REALTOR')
);

-- "Minhas propostas" e consultado pelos dois lados: pelo cliente e pelo corretor dono.
CREATE INDEX idx_proposals_customer_id ON proposals (customer_id);
CREATE INDEX idx_proposals_listing_id  ON proposals (listing_id);
CREATE INDEX idx_proposals_status      ON proposals (status);

-- Thread ancorado na proposta, e nao chat livre entre usuarios: a conversa fica junto do que
-- esta sendo negociado. Contraproposta e uma mensagem sugerindo outro valor, nao um estado.
CREATE TABLE proposal_messages (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    proposal_id     BIGINT        NOT NULL,
    author_id       BIGINT        NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_proposal_messages           PRIMARY KEY (id),
    CONSTRAINT fk_proposal_messages_proposals FOREIGN KEY (proposal_id) REFERENCES proposals (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposal_messages_users     FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_proposal_messages_proposal_id ON proposal_messages (proposal_id);
