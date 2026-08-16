-- Nucleo de anuncios: o anuncio comum e os dois detalhamentos possiveis.
--
-- listings guarda o que imovel e automovel tem em comum (autor, preco, localizacao,
-- situacao); properties e vehicles guardam o que so faz sentido em um dos dois. Os
-- detalhamentos compartilham a chave primaria do anuncio -- e a mesma identidade, nao uma
-- sequencia propria: um imovel nao existe fora do anuncio que o descreve, e a PK
-- compartilhada e o que impede dois detalhamentos para o mesmo listing.
--
-- Os CHECK de dominio repetem os enums da aplicacao de proposito: o banco e a ultima
-- fronteira de integridade, e um INSERT feito fora da aplicacao nao pode gravar
-- 'ATIVO' na coluna status.

CREATE TABLE listings (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    realtor_id      BIGINT        NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    operation_type  VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    title           VARCHAR(255)  NOT NULL,
    description     VARCHAR(4000) NOT NULL,
    price           NUMERIC(15,2) NOT NULL,
    neighborhood    VARCHAR(255)  NOT NULL,
    city            VARCHAR(255)  NOT NULL,
    state           VARCHAR(2)    NOT NULL,
    latitude        NUMERIC(9,6)  NOT NULL,
    longitude       NUMERIC(9,6)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_listings                    PRIMARY KEY (id),
    CONSTRAINT fk_listings_users              FOREIGN KEY (realtor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_listings_type               CHECK (type IN ('PROPERTY', 'VEHICLE')),
    CONSTRAINT ck_listings_operation_type     CHECK (operation_type IN ('SALE', 'RENT')),
    CONSTRAINT ck_listings_status             CHECK (status IN ('ACTIVE', 'PAUSED', 'REMOVED', 'COMPLETED')),
    CONSTRAINT ck_listings_price_positive     CHECK (price > 0),
    CONSTRAINT ck_listings_state_length       CHECK (char_length(state) = 2),
    CONSTRAINT ck_listings_latitude_range     CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_listings_longitude_range    CHECK (longitude BETWEEN -180 AND 180),
    -- Aluguel so existe para imovel: alugar veiculo e operacao de locadora, nao de corretor.
    CONSTRAINT ck_listings_vehicle_only_sale  CHECK (type <> 'VEHICLE' OR operation_type = 'SALE')
);

-- "Meus anuncios" (do corretor) e a exclusao em cascata passam por realtor_id.
CREATE INDEX idx_listings_realtor_id ON listings (realtor_id);

-- O catalogo publico sempre filtra por situacao e tipo antes de qualquer outro criterio.
CREATE INDEX idx_listings_status_type ON listings (status, type);

CREATE INDEX idx_listings_city_state ON listings (city, state);

CREATE INDEX idx_listings_price ON listings (price);

-- A busca por raio calcula a distancia sobre um recorte previo de coordenadas; sem este
-- indice o recorte seria uma varredura completa da tabela.
CREATE INDEX idx_listings_latitude_longitude ON listings (latitude, longitude);

CREATE TABLE properties (
    listing_id      BIGINT        NOT NULL,
    property_type   VARCHAR(20)   NOT NULL,
    total_area      NUMERIC(12,2) NOT NULL,
    bedrooms        INTEGER,
    bathrooms       INTEGER,
    garage_spots    INTEGER,
    condominium_fee NUMERIC(15,2),
    iptu            NUMERIC(15,2),
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_properties                      PRIMARY KEY (listing_id),
    CONSTRAINT fk_properties_listings             FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT ck_properties_property_type        CHECK (property_type IN ('APARTMENT', 'HOUSE', 'LAND', 'COMMERCIAL', 'FARM')),
    CONSTRAINT ck_properties_total_area_positive  CHECK (total_area > 0),
    CONSTRAINT ck_properties_bedrooms_range       CHECK (bedrooms IS NULL OR bedrooms >= 0),
    CONSTRAINT ck_properties_bathrooms_range      CHECK (bathrooms IS NULL OR bathrooms >= 0),
    CONSTRAINT ck_properties_garage_spots_range   CHECK (garage_spots IS NULL OR garage_spots >= 0),
    CONSTRAINT ck_properties_condominium_fee_range CHECK (condominium_fee IS NULL OR condominium_fee >= 0),
    CONSTRAINT ck_properties_iptu_range           CHECK (iptu IS NULL OR iptu >= 0)
);

-- Comodidades como lista fechada em tabela propria, e nao como texto livre: e o que torna o
-- filtro "com piscina" confiavel. Sem repository proprio -- @ElementCollection do imovel.
CREATE TABLE property_amenities (
    listing_id BIGINT      NOT NULL,
    amenity    VARCHAR(30) NOT NULL,
    CONSTRAINT pk_property_amenities             PRIMARY KEY (listing_id, amenity),
    CONSTRAINT fk_property_amenities_properties  FOREIGN KEY (listing_id) REFERENCES properties (listing_id) ON DELETE CASCADE,
    CONSTRAINT ck_property_amenities_amenity     CHECK (amenity IN (
        'POOL', 'BARBECUE_GRILL', 'GYM', 'ELEVATOR', 'PLAYGROUND',
        'PARTY_ROOM', 'CONCIERGE', 'FURNISHED', 'BALCONY', 'PETS_ALLOWED'))
);

CREATE TABLE vehicles (
    listing_id       BIGINT       NOT NULL,
    vehicle_type     VARCHAR(20)  NOT NULL,
    brand            VARCHAR(255) NOT NULL,
    model            VARCHAR(255) NOT NULL,
    color            VARCHAR(255) NOT NULL,
    manufacture_year INTEGER      NOT NULL,
    model_year       INTEGER      NOT NULL,
    mileage_km       INTEGER      NOT NULL,
    fuel_type        VARCHAR(20)  NOT NULL,
    transmission     VARCHAR(20)  NOT NULL,
    condition        VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    last_updated_at  TIMESTAMP    NOT NULL,
    created_by       VARCHAR(255),
    last_updated_by  VARCHAR(255),
    CONSTRAINT pk_vehicles                    PRIMARY KEY (listing_id),
    CONSTRAINT fk_vehicles_listings           FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT ck_vehicles_vehicle_type       CHECK (vehicle_type IN ('CAR', 'MOTORCYCLE', 'TRUCK', 'VAN')),
    CONSTRAINT ck_vehicles_fuel_type          CHECK (fuel_type IN ('GASOLINE', 'ETHANOL', 'FLEX', 'DIESEL', 'ELECTRIC', 'HYBRID')),
    CONSTRAINT ck_vehicles_transmission       CHECK (transmission IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT ck_vehicles_condition          CHECK (condition IN ('NEW', 'USED')),
    CONSTRAINT ck_vehicles_mileage_km_range   CHECK (mileage_km >= 0),
    CONSTRAINT ck_vehicles_manufacture_year_range CHECK (manufacture_year >= 1900),
    -- O ano do modelo e o de fabricacao ou o seguinte; nunca anterior.
    CONSTRAINT ck_vehicles_model_year_range   CHECK (model_year >= manufacture_year)
);

-- Guarda a chave do objeto no storage, nunca a URL: o endereco publico muda com o ambiente
-- (MinIO local, S3 em producao) e gravar a URL invalidaria as linhas na primeira migracao
-- de infraestrutura.
CREATE TABLE listing_photos (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY,
    listing_id      BIGINT       NOT NULL,
    storage_key     VARCHAR(255) NOT NULL,
    position        INTEGER      NOT NULL,
    cover           BOOLEAN      NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    last_updated_at TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_listing_photos                     PRIMARY KEY (id),
    CONSTRAINT fk_listing_photos_listings            FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT uk_listing_photos_storage_key         UNIQUE (storage_key),
    CONSTRAINT uk_listing_photos_listing_id_position UNIQUE (listing_id, position),
    CONSTRAINT ck_listing_photos_position_positive   CHECK (position > 0)
);

CREATE INDEX idx_listing_photos_listing_id ON listing_photos (listing_id);
