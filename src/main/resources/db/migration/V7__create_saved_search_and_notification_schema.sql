-- Busca salva e notificacao.
--
-- Uma unica tabela de busca atende os dois usos do dominio -- alerta privado e anuncio de
-- procura publico -- distinguidos pela coluna visibility. Os criterios e o casamento sao
-- identicos nos dois casos; duas tabelas duplicariam ambos.
--
-- Os criterios especificos de cada tipo ficam em tabelas dependentes com chave compartilhada,
-- mesmo padrao de properties/vehicles para o anuncio.

CREATE TABLE saved_searches (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    customer_id     BIGINT        NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    visibility      VARCHAR(20)   NOT NULL,
    title           VARCHAR(255)  NOT NULL,
    operation_type  VARCHAR(20),
    min_price       NUMERIC(15,2),
    max_price       NUMERIC(15,2),
    city            VARCHAR(255),
    state           VARCHAR(2),
    neighborhood    VARCHAR(255),
    latitude        NUMERIC(9,6),
    longitude       NUMERIC(9,6),
    radius_km       INTEGER,
    active          BOOLEAN       NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_saved_searches                 PRIMARY KEY (id),
    CONSTRAINT fk_saved_searches_users           FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_saved_searches_type            CHECK (type IN ('PROPERTY', 'VEHICLE')),
    CONSTRAINT ck_saved_searches_visibility      CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    CONSTRAINT ck_saved_searches_operation_type  CHECK (operation_type IS NULL OR operation_type IN ('SALE', 'RENT')),
    CONSTRAINT ck_saved_searches_min_price_range CHECK (min_price IS NULL OR min_price >= 0),
    CONSTRAINT ck_saved_searches_max_price_range CHECK (max_price IS NULL OR max_price >= 0),
    -- Faixa invertida nao filtraria nada: o banco recusa em vez de guardar uma busca morta.
    CONSTRAINT ck_saved_searches_price_order     CHECK (min_price IS NULL OR max_price IS NULL OR min_price <= max_price),
    CONSTRAINT ck_saved_searches_state_length    CHECK (state IS NULL OR char_length(state) = 2),
    CONSTRAINT ck_saved_searches_latitude_range  CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_saved_searches_longitude_range CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_saved_searches_radius_positive CHECK (radius_km IS NULL OR radius_km > 0),
    -- Raio sem centro nao define recorte nenhum.
    CONSTRAINT ck_saved_searches_radius_center   CHECK (radius_km IS NULL OR (latitude IS NOT NULL AND longitude IS NOT NULL))
);

CREATE INDEX idx_saved_searches_customer_id ON saved_searches (customer_id);

-- O casamento roda a cada anuncio publicado e sempre recorta por busca ativa e tipo.
CREATE INDEX idx_saved_searches_active_type ON saved_searches (active, type);

-- A vitrine de procura lista as buscas publicas.
CREATE INDEX idx_saved_searches_visibility ON saved_searches (visibility);

CREATE TABLE property_search_criteria (
    saved_search_id BIGINT        NOT NULL,
    property_type   VARCHAR(20),
    min_bedrooms    INTEGER,
    min_bathrooms   INTEGER,
    min_total_area  NUMERIC(12,2),
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_property_search_criteria                PRIMARY KEY (saved_search_id),
    CONSTRAINT fk_property_search_criteria_saved_searches FOREIGN KEY (saved_search_id) REFERENCES saved_searches (id) ON DELETE CASCADE,
    CONSTRAINT ck_property_search_criteria_property_type  CHECK (property_type IS NULL OR property_type IN ('APARTMENT', 'HOUSE', 'LAND', 'COMMERCIAL', 'FARM')),
    CONSTRAINT ck_property_search_criteria_bedrooms_range CHECK (min_bedrooms IS NULL OR min_bedrooms >= 0),
    CONSTRAINT ck_property_search_criteria_bathrooms_range CHECK (min_bathrooms IS NULL OR min_bathrooms >= 0),
    CONSTRAINT ck_property_search_criteria_area_positive  CHECK (min_total_area IS NULL OR min_total_area > 0)
);

CREATE TABLE vehicle_search_criteria (
    saved_search_id      BIGINT      NOT NULL,
    vehicle_type         VARCHAR(20),
    brand                VARCHAR(255),
    max_mileage_km       INTEGER,
    min_manufacture_year INTEGER,
    created_at           TIMESTAMP   NOT NULL,
    last_updated_at      TIMESTAMP   NOT NULL,
    created_by           VARCHAR(255),
    last_updated_by      VARCHAR(255),
    CONSTRAINT pk_vehicle_search_criteria                 PRIMARY KEY (saved_search_id),
    CONSTRAINT fk_vehicle_search_criteria_saved_searches  FOREIGN KEY (saved_search_id) REFERENCES saved_searches (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicle_search_criteria_vehicle_type    CHECK (vehicle_type IS NULL OR vehicle_type IN ('CAR', 'MOTORCYCLE', 'TRUCK', 'VAN')),
    CONSTRAINT ck_vehicle_search_criteria_mileage_range   CHECK (max_mileage_km IS NULL OR max_mileage_km >= 0),
    CONSTRAINT ck_vehicle_search_criteria_year_range      CHECK (min_manufacture_year IS NULL OR min_manufacture_year >= 1900)
);

-- Guarda a mensagem ja redigida: o historico continua legivel mesmo que o anuncio ou a
-- proposta que a originou mude ou saia do ar.
CREATE TABLE notifications (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    recipient_id    BIGINT        NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    read            BOOLEAN       NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_notifications       PRIMARY KEY (id),
    CONSTRAINT fk_notifications_users FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Os tipos dos clusters seguintes ja entram aqui para que o CHECK nao precise ser
    -- reescrito a cada cluster da Fase 2.
    CONSTRAINT ck_notifications_type  CHECK (type IN (
        'SEARCH_MATCH', 'NEW_PROPOSAL', 'PROPOSAL_STATUS_CHANGED', 'NEW_PROPOSAL_MESSAGE',
        'NEW_REVIEW', 'NEW_FOLLOWER', 'LISTING_REPORTED'))
);

-- "Minhas notificacoes" abre pelas nao lidas, mais recentes primeiro.
CREATE INDEX idx_notifications_recipient_id_read ON notifications (recipient_id, read);
