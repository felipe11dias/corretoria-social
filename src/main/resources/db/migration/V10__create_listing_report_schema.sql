-- Moderacao: denuncia de anuncio.
--
-- E o contrapeso da publicacao imediata. A plataforma confia no corretor para publicar sem
-- fila de aprovacao, e confia na comunidade para apontar o que estiver errado -- sem o
-- segundo, a confianca do primeiro ficaria sem resposta.
--
-- Os desfechos sao registrados em vez de a linha ser apagada: saber que um anuncio ja foi
-- denunciado e liberado e o que evita reabrir a mesma discussao a cada nova denuncia.
--
-- Quem decidiu fica em last_updated_by, que ja existe pela auditoria; uma coluna de
-- "revisado por" seria a mesma informacao escrita duas vezes.

CREATE TABLE listing_reports (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY,
    listing_id      BIGINT        NOT NULL,
    reporter_id     BIGINT        NOT NULL,
    reason          VARCHAR(30)   NOT NULL,
    detail          VARCHAR(2000),
    status          VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    last_updated_at TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    last_updated_by VARCHAR(255),
    CONSTRAINT pk_listing_reports          PRIMARY KEY (id),
    CONSTRAINT fk_listing_reports_listings FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_reports_users    FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_listing_reports_reason   CHECK (reason IN (
        'MISLEADING_INFO', 'PROHIBITED_ITEM', 'SUSPECTED_FRAUD',
        'INAPPROPRIATE_CONTENT', 'OTHER')),
    CONSTRAINT ck_listing_reports_status   CHECK (status IN ('OPEN', 'REVIEWED', 'DISMISSED'))
);

-- A fila de moderacao abre sempre pelas denuncias em aberto, das mais antigas para as mais
-- novas: quem esperou mais e atendido primeiro.
CREATE INDEX idx_listing_reports_status_created_at ON listing_reports (status, created_at);

-- Quantas denuncias um anuncio acumulou e a pergunta seguinte do moderador.
CREATE INDEX idx_listing_reports_listing_id ON listing_reports (listing_id);
