# Requisitos — Corretoria Fase 2: Domínio de Anúncios (Imóveis e Automóveis)

**Autor:** Felipe Dias Mac Dowell
**Data:** 2026-07-31
**Depende de:** [`relatorio-corretoria-v1_0.md`](../relatorios/relatorio-corretoria-v1_0.md) (fundação de identidade — autenticação, usuários, papéis —, concluída em 26/07/2026, 15 etapas, 136 testes, 32 regras de ArchUnit) e [`requisitos-corretoria-v1_0.md`](requisitos-corretoria-v1_0.md) (mesmos requisitos, em formato de requisitos)
**Natureza deste documento:** requisitos e modelagem — ainda não há código desta fase implementado. É o ponto de partida para a Fase 2, do mesmo jeito que um relatório de etapas registrará a execução depois.

## 1. Contexto e objetivo

A Fase 1 entregou uma **plataforma de identidade** pura: autocadastro,
autenticação JWT, autorização por posse, gestão de usuários por admin. Não
existe nenhuma entidade, rota, VO ou termo de glossário relacionado a
imóvel, automóvel, anúncio ou busca — a Fase 1 é folha em branco sobre esse
domínio, deliberadamente.

O objetivo da Fase 2 é modelar e (nas etapas seguintes) implementar um
**ambiente social de corretores e clientes**: corretores publicam anúncios
de imóveis e automóveis; clientes buscam, se conectam e negociam com
corretores por mecanismos clássicos (busca e filtro) e diferenciais (busca
salva com alerta, anúncio de procura público, propostas estruturadas,
reputação, feed social). Este documento é o resultado de um processo de
elicitação de requisitos conduzido em 5 rodadas de perguntas dirigidas —
20 decisões de modelo de dados e de escopo de funcionalidade, cada uma
confirmada explicitamente antes de entrar aqui.

## 2. Estado atual da fundação (resumo)

- **Stack:** Java 21, Spring Boot 3.5.16, PostgreSQL 16, Flyway, MapStruct,
  springdoc-openapi, JWT (jjwt), Spring Security, HATEOAS.
- **Arquitetura em camadas** (ArchUnit, 32 regras, poder de veto):
  `controller → service → repository → entity/enums`, transversais `vo`
  (records), `mapper` (MapStruct), `exception` (ProblemDetail/RFC 7807),
  `security`, `config`, `util`. Toda a Fase 2 entra nessa mesma estrutura —
  não há módulo à parte. As 32 regras são genéricas por pacote/sufixo, então
  cobrem as novas classes automaticamente; nenhuma regra nova é necessária.
- **Identidade:** um único `User` (sem classes `Realtor`/`Customer`/`Admin`
  separadas); papel via `Set<Role>` (`ManyToMany`) com `RoleName` =
  `ROLE_ADMIN` / `ROLE_REALTOR` / `ROLE_CUSTOMER`. `Address` é dependente de
  `User` (sem repository próprio, cascade + orphanRemoval). Autorização por
  posse via `UserSecurity.isSelf` (nunca confia em id do payload) + escopo
  admin via `hasRole('ADMIN')`.
- **Contrato:** sem DTO (proibido pelo nome) — só VO `record` (`vo.shared`
  para VO de valor, `vo.v1.request`/`vo.v1.response` para contrato de API,
  sufixo `Request`/`Response` obrigatório). Entity↔VO exclusivo via
  MapStruct.
- **Segurança hoje:** tudo exige token exceto `/auth/**` e `POST /users`
  (`.anyRequest().authenticated()`). Essa política muda na Fase 2 (regra 5,
  seção 6).
- **Pendência conhecida, fora de escopo aqui:** `PasswordResetToken` já tem
  entidade/tabela/repository migrados, mas nenhum service/controller usa o
  fluxo ainda.

## 3. Papéis e matriz de permissões (extensão da Fase 1)

| Ação | Anônimo | `ROLE_CUSTOMER` | `ROLE_REALTOR` | `ROLE_ADMIN` |
|---|---|---|---|---|
| Listar/buscar/ver anúncio | ✅ | ✅ | ✅ | ✅ |
| Publicar/editar/pausar/remover anúncio | ❌ | ❌ | ✅ (só o próprio) | ✅ (qualquer) |
| Criar busca salva (privada ou pública) | ❌ | ✅ | ❌ | — |
| Ver buscas públicas (vitrine de procura) | ❌ | ✅ | ✅ | ✅ |
| Propor sobre um anúncio | ❌ | ✅ | ❌ | — |
| Propor a partir de busca pública | ❌ | ❌ | ✅ | — |
| Aceitar/recusar/retirar proposta | ❌ | ✅ (a que participa) | ✅ (a que participa) | — |
| Avaliar corretor | ❌ | ✅ (se houver `Proposal` prévia) | ❌ | — |
| Favoritar anúncio / seguir corretor | ❌ | ✅ | ❌ | — |
| Publicar post no mural | ❌ | ❌ | ✅ (só o próprio) | — |
| Curtir/comentar post | ❌ | ✅ | ✅ | ✅ |
| Denunciar anúncio | ❌ | ✅ | ✅ | ✅ |
| Revisar/decidir denúncia | ❌ | ❌ | ❌ | ✅ |

## 4. Extensão do glossário de nomenclatura

A skill `corretoria-naming-standards` precisa incorporar este bloco **antes
do primeiro código** — é a própria regra da skill quando um conceito novo
não está coberto.

| Conceito (PT) | Termo canônico | Proibido |
|---|---|---|
| Anúncio | `listing` / `Listing` | `ad`, `announcement`, `anuncio` |
| Imóvel | `property` / `Property` | `realEstate`, `estate`, `imovel` |
| Automóvel/veículo | `vehicle` / `Vehicle` | `car` (é só um `VehicleType`), `automovel` |
| Foto do anúncio | `photo` / `ListingPhoto` | `image`, `picture`, `foto` |
| Busca salva / anúncio de procura | `savedSearch` / `SavedSearch` | `searchRequest` (colide com sufixo de VO), `wantedAd`, `busca` |
| Proposta | `proposal` / `Proposal` | `offer`, `bid`, `oferta` |
| Mensagem de proposta | `proposalMessage` / `ProposalMessage` | `chatMessage`, `mensagem` |
| Avaliação | `review` / `Review` | `rating`, `feedback`, `avaliacao` |
| Favorito | `favorite` / `ListingFavorite` | `bookmark`, `like` (reservado a post) |
| Seguir corretor | `follow` / `RealtorFollow` | `subscription`, `seguidor` |
| Publicação do corretor | `post` / `RealtorPost` | `update`, `publicacao` |
| Denúncia | `report` / `ListingReport` | `complaint`, `flag`, `denuncia` |
| Notificação | `notification` / `Notification` | `alert`, `notificacao` |

> **Nota de nomenclatura:** o conceito de busca foi nomeado `SavedSearch`,
> não `SearchRequest` — o sufixo `Request` é reservado pela skill a VOs de
> contrato (`vo.v1.request`), então `SearchRequest` como nome de entidade
> colidiria com o VO de criação (`SearchRequestCreationRequest`, "Request"
> duplicado). `SavedSearch` também é o termo consagrado no setor (Zillow e
> Redfin chamam exatamente isso de "saved search").

## 5. Modelo de dados

Convenções mantidas da Fase 1: `id` `Long` `IDENTITY`; toda entidade com
ciclo de vida próprio estende `Auditable`; dinheiro em `BigDecimal`; enum
com `@Enumerated(STRING)`; constraints nomeadas explicitamente
(`pk_`/`fk_`/`uk_`/`ck_`/`idx_` + tabela + colunas).

### 5.1 Cluster A — Núcleo de anúncios

**`Listing`** (`listings`) — agregado raiz, repository próprio.

| Campo | Tipo | Regra |
|---|---|---|
| realtor | `User` (FK) | NOT NULL; deve ter `ROLE_REALTOR` (validado no service) |
| type | `ListingType` (`PROPERTY`, `VEHICLE`) | NOT NULL |
| operationType | `OperationType` (`SALE`, `RENT`) | NOT NULL; `RENT` só se `type=PROPERTY` (CHECK) |
| status | `ListingStatus` (`ACTIVE`, `PAUSED`, `REMOVED`, `COMPLETED`) | NOT NULL, default `ACTIVE` |
| title, description | `String` | NOT NULL |
| price | `BigDecimal` | NOT NULL, > 0 (CHECK) |
| neighborhood, city, state | `String` | NOT NULL |
| latitude, longitude | `BigDecimal` | NOT NULL |
| + `Auditable` | | `createdAt` também é a data de publicação (publicação é imediata) |

**`Property`** (`properties`, PK/FK `listing_id` — dependente de `Listing`,
sem repository próprio, cascade+orphanRemoval, mesmo padrão de `Address`).

| Campo | Tipo | Regra |
|---|---|---|
| propertyType | `PropertyType` (`APARTMENT`, `HOUSE`, `LAND`, `COMMERCIAL`, `FARM`) | NOT NULL |
| totalArea | `BigDecimal` (m²) | NOT NULL, > 0 |
| bedrooms, bathrooms, garageSpots | `Integer` | opcionais |
| condominiumFee, iptu | `BigDecimal` | opcionais |
| amenities | `Set<PropertyAmenity>` | `@ElementCollection`, tabela `property_amenities` (lista fechada em enum, sem EAV) |

**`Vehicle`** (`vehicles`, PK/FK `listing_id` — mesmo padrão de `Property`).

| Campo | Tipo | Regra |
|---|---|---|
| vehicleType | `VehicleType` (`CAR`, `MOTORCYCLE`, `TRUCK`, `VAN`) | NOT NULL |
| brand, model, color | `String` | NOT NULL |
| manufactureYear, modelYear | `Integer` | NOT NULL |
| mileageKm | `Integer` | NOT NULL, >= 0 |
| fuelType | `FuelType` (`GASOLINE`, `ETHANOL`, `FLEX`, `DIESEL`, `ELECTRIC`, `HYBRID`) | NOT NULL |
| transmission | `TransmissionType` (`MANUAL`, `AUTOMATIC`) | NOT NULL |
| condition | `VehicleCondition` (`NEW`, `USED`) | NOT NULL |

**`ListingPhoto`** (`listing_photos`, dependente de `Listing`).

| Campo | Tipo | Regra |
|---|---|---|
| listing | FK | NOT NULL |
| storageKey | `String` | NOT NULL — chave do objeto no storage S3-compatible |
| position | `Integer` | NOT NULL — ordem de exibição |
| cover | `boolean` | default `false` — foto de capa |

### 5.2 Cluster B — Busca salva e notificações

**`SavedSearch`** (`saved_searches`) — agregado raiz, repository próprio.

| Campo | Tipo | Regra |
|---|---|---|
| customer | `User` (FK) | NOT NULL; deve ter `ROLE_CUSTOMER` |
| type | `ListingType` | NOT NULL |
| visibility | `SearchVisibility` (`PRIVATE`, `PUBLIC`) | NOT NULL |
| title | `String` | NOT NULL — texto do "anúncio de procura" quando `PUBLIC` |
| minPrice, maxPrice | `BigDecimal` | opcionais |
| operationType | `OperationType` | opcional (nulo = qualquer) |
| city, state, neighborhood | `String` | opcionais |
| latitude, longitude, radiusKm | `BigDecimal`/`Integer` | opcionais, habilita busca por raio |
| active | `boolean` | NOT NULL, default `true` — pausar sem apagar |

**`PropertySearchCriteria`** (`property_search_criteria`, PK/FK
`saved_search_id`, dependente): `minBedrooms`, `minBathrooms`,
`minTotalArea`, `propertyType` (todos opcionais).

**`VehicleSearchCriteria`** (`vehicle_search_criteria`, PK/FK
`saved_search_id`, dependente): `brand`, `maxMileageKm`,
`minManufactureYear`, `vehicleType` (todos opcionais).

**`Notification`** (`notifications`) — agregado raiz, repository próprio.

| Campo | Tipo | Regra |
|---|---|---|
| recipient | `User` (FK) | NOT NULL |
| type | `NotificationType` (`SEARCH_MATCH`, `NEW_PROPOSAL`, `PROPOSAL_STATUS_CHANGED`, `NEW_PROPOSAL_MESSAGE`, `NEW_REVIEW`, `NEW_FOLLOWER`, `LISTING_REPORTED`) | NOT NULL |
| message | `String` | NOT NULL |
| read | `boolean` | NOT NULL, default `false` |

### 5.3 Cluster C — Negociação

**`Proposal`** (`proposals`) — agregado raiz, repository próprio.

| Campo | Tipo | Regra |
|---|---|---|
| listing | `Listing` (FK) | NOT NULL |
| customer | `User` (FK) | NOT NULL — contraparte cliente, sempre, independente de quem iniciou |
| initiator | `ProposalInitiator` (`CUSTOMER`, `REALTOR`) | NOT NULL |
| originSavedSearch | `SavedSearch` (FK) | opcional — preenchido quando corretor iniciou a partir de busca pública |
| amount | `BigDecimal` | NOT NULL, > 0 |
| status | `ProposalStatus` (`PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`) | NOT NULL, default `PENDING` |

Invariante: `customer.id ≠ listing.realtor.id` (corretor não propõe no
próprio anúncio).

**`ProposalMessage`** (`proposal_messages`, dependente de `Proposal`):
`author` (`User` FK), `content` (`String` NOT NULL). `createdAt` (via
`Auditable`) é o timestamp da mensagem.

### 5.4 Cluster D — Social

**`Review`** (`reviews`) — agregado raiz, repository próprio.

| Campo | Tipo | Regra |
|---|---|---|
| realtor | `User` (FK) | NOT NULL — sujeito da avaliação |
| customer | `User` (FK) | NOT NULL — autor |
| score | `Integer` | NOT NULL, CHECK `1..5` |
| comment | `String` | opcional |

`UNIQUE(realtor_id, customer_id)` — uma avaliação por par (cliente
atualiza a nota existente em vez de acumular várias). Elegibilidade
(existe ao menos um `Proposal` entre as partes) é validada no service no
momento da criação, sem guardar qual `Proposal` a habilitou.

**`ListingFavorite`** (`listing_favorites`) — junção simples: `customer`
(FK), `listing` (FK), `createdAt` simples (não o pacote `Auditable`
inteiro — favorito não é "atualizado", só criado/removido, mesmo
raciocínio de `user_roles`). `UNIQUE(customer_id, listing_id)`. **Tem
repository próprio** (diferente de `user_roles`) porque é consultado
diretamente (ex.: "meus favoritos").

**`RealtorFollow`** (`realtor_follows`) — mesmo padrão: `customer` (FK),
`realtor` (FK), `createdAt` simples, `UNIQUE(customer_id, realtor_id)`,
repository próprio.

**`RealtorPost`** (`realtor_posts`) — agregado raiz, repository próprio:
`realtor` (FK), `content` (`String` NOT NULL).

**`RealtorPostComment`** (`realtor_post_comments`, dependente de
`RealtorPost`): `author` (`User` FK), `content` (NOT NULL).

**`RealtorPostLike`** (`realtor_post_likes`) — junção simples: `post`
(FK), `user` (FK), `createdAt` simples, `UNIQUE(post_id, user_id)`.
Curtir/comentar é liberado a **qualquer usuário autenticado** (cliente ou
corretor); favoritar anúncio e seguir corretor ficam restritos a
`ROLE_CUSTOMER`.

### 5.5 Cluster E — Moderação

**`ListingReport`** (`listing_reports`) — agregado raiz, repository
próprio.

| Campo | Tipo | Regra |
|---|---|---|
| listing | `Listing` (FK) | NOT NULL |
| reporter | `User` (FK) | NOT NULL |
| reason | `ReportReason` (`MISLEADING_INFO`, `PROHIBITED_ITEM`, `SUSPECTED_FRAUD`, `INAPPROPRIATE_CONTENT`, `OTHER`) | NOT NULL |
| detail | `String` | opcional |
| status | `ReportStatus` (`OPEN`, `REVIEWED`, `DISMISSED`) | NOT NULL, default `OPEN` |

`lastUpdatedBy` (herdado de `Auditable`) já identifica qual admin revisou —
sem campo extra de "revisado por".

## 6. Regras de negócio consolidadas

1. Apenas usuários com `ROLE_REALTOR` publicam `Listing`; `ROLE_CUSTOMER`
   nunca oferta, só busca.
2. Um `Listing` é de `Property` OU `Vehicle`, nunca os dois.
3. `Vehicle` sempre tem `operationType = SALE` — aluguel de veículo fora de
   escopo (é operação de locadora, não de corretor individual).
4. Publicação de anúncio é imediata, sem fila de aprovação — mesmo padrão
   de confiança já usado no autocadastro de usuários da Fase 1.
5. Catálogo de anúncios é público (leitura sem token); toda ação
   (favoritar, propor, avaliar, seguir, publicar, denunciar) exige
   autenticação.
6. Apenas `ROLE_CUSTOMER` cria `SavedSearch`; pode ser privada (só alerta
   o dono) ou pública (vitrine de "anúncio de procura" para corretores).
7. `Proposal` pode ser iniciada pelo cliente (sobre um `Listing`) ou pelo
   corretor (a partir de uma `SavedSearch` pública de um cliente).
8. Corretor nunca propõe sobre o próprio anúncio.
9. Contraproposta não é um estado formal de `Proposal` — é uma nova
   mensagem no thread da proposta sugerindo outro valor.
10. `Review` só pode ser criada se existir ao menos uma `Proposal` entre
    cliente e corretor (não precisa ter sido aceita); uma avaliação por
    par, atualizável.
11. `ListingReport` pode ser aberto por qualquer usuário autenticado; só
    `ROLE_ADMIN` decide o status final (`REVIEWED`/`DISMISSED`).
12. Notificações cobrem: match de busca salva, nova proposta, mudança de
    status de proposta, nova mensagem, nova avaliação, novo seguidor,
    denúncia recebida — entregues in-app e por e-mail.
13. Nenhuma entidade nova expõe dado sensível em `Response` (mesma regra
    já vigente hoje via ArchUnit).
14. Sem transação/fechamento formal: `Listing.status = COMPLETED` é só
    sinalização manual do corretor, sem histórico de venda/comissão.
15. Sem destaque pago nem plano de assinatura nesta fase.

## 7. Esboço da API REST (`/api/v1`, mesmo padrão da Fase 1)

| Recurso | Método + rota | Auth | Notas |
|---|---|---|---|
| Anúncios | `POST /listings` | `ROLE_REALTOR` | corpo inclui `property` ou `vehicle` conforme `type` |
| | `GET /listings` | Público | paginado; filtros comuns (type, operationType, city, state, price) + por raio (lat/long/radiusKm) + específicos do tipo |
| | `GET /listings/{id}` | Público | |
| | `PUT /listings/{id}` | Dono | |
| | `PATCH /listings/{id}/status` | Dono | `ACTIVE`/`PAUSED`/`REMOVED`/`COMPLETED` |
| | `POST /listings/{id}/photos` | Dono | multipart, delega ao storage |
| | `DELETE /listings/{id}/photos/{photoId}` | Dono | |
| Buscas salvas | `POST /saved-searches` | `ROLE_CUSTOMER` | |
| | `GET /saved-searches` (minhas) | Dono | |
| | `GET /saved-searches/public` | Público/autenticado | vitrine de "anúncio de procura" |
| | `PUT`/`DELETE /saved-searches/{id}` | Dono | |
| Propostas | `POST /proposals` | Autenticado | `initiator` inferido do papel de quem chama |
| | `GET /proposals` (minhas) | Autenticado | |
| | `PATCH /proposals/{id}/status` | Contraparte | aceitar/recusar/retirar |
| | `POST /proposals/{id}/messages` | Participante | |
| Avaliações | `POST /realtors/{id}/reviews` | `ROLE_CUSTOMER` elegível | 403 se não houver `Proposal` prévia |
| | `GET /realtors/{id}/reviews` | Público | |
| Favoritos | `PUT`/`DELETE /listings/{id}/favorite` | `ROLE_CUSTOMER` | |
| Seguir | `PUT`/`DELETE /realtors/{id}/follow` | `ROLE_CUSTOMER` | |
| Mural | `POST /realtors/{id}/posts` | Dono (`ROLE_REALTOR`) | |
| | `GET /realtors/{id}/posts` | Público | |
| | `POST /posts/{id}/comments` | Autenticado | |
| | `PUT`/`DELETE /posts/{id}/like` | Autenticado | |
| Denúncias | `POST /listings/{id}/reports` | Autenticado | |
| | `GET /reports` | `ROLE_ADMIN` | fila de moderação |
| | `PATCH /reports/{id}/status` | `ROLE_ADMIN` | |
| Notificações | `GET /notifications` (minhas) | Autenticado | |
| | `PATCH /notifications/{id}/read` | Dono | |

## 8. Funcionalidades: clássicas vs. diferenciais

**Clássicas** (consolidadas em qualquer portal do setor): publicação de
anúncio com fotos/preço/descrição; busca e filtro por tipo, localização e
faixa de preço; anúncio de venda ou aluguel (imóvel); detalhe do anúncio
com dados do corretor responsável; favoritar anúncios; paginação/ordenação
(reaproveita o padrão já existente).

**Diferenciais** (o que distingue a Corretoria): anúncio de procura
público — mercado reverso, corretor também "caça" cliente; busca salva com
alerta automático; corretor pode iniciar proposta a partir da busca
pública de um cliente; geolocalização com busca por raio/mapa; reputação
de corretor com avaliação *gated* por interação real (anti-fraude, análogo
a "compra verificada"); feed/mural do corretor com curtidas e comentários
— camada social genuína, não só marketplace; seguir corretores
específicos; notificação in-app + e-mail.

## 9. Considerações técnicas e de arquitetura

- **Novas dependências:** SDK de storage S3-compatible (ex.
  `software.amazon.awssdk:s3`) e `spring-boot-starter-mail`.
- **Nova infraestrutura local** (docker-compose): serviço MinIO
  (S3-compatible, sem custo em dev) + variáveis `corretoria.storage.*`;
  servidor SMTP de desenvolvimento (ex. Mailpit/MailHog).
- **Busca por raio em PostgreSQL puro:** fórmula de Haversine em `@Query`
  nativa, ou extensão `cube`/`earthdistance` (mais precisa, requer `CREATE
  EXTENSION` numa migration) — decisão de implementação a refinar na etapa
  técnica, não bloqueia o modelo de dados.
- **Matching assíncrono:** publicar `Listing` dispara evento
  (`ApplicationEventPublisher` + `@TransactionalEventListener(phase =
  AFTER_COMMIT)` + `@Async`) que varre `SavedSearch` ativas compatíveis e
  gera `Notification` (+ e-mail). Mantém `ListingService` focado (SRP) — o
  matching vive em service próprio.
- **ArchUnit:** nenhuma regra nova estrutural necessária — as 32 regras
  atuais são genéricas por pacote/sufixo e cobrem as novas classes
  automaticamente.
- **CHECK constraints a replicar no Flyway** (mesmo estilo de `V1`):
  coerência `type`/`operationType` (`Vehicle` sempre `SALE`), `score`
  `1..5` em `reviews`, `price`/`amount` `> 0`.
- **Glossário:** a skill `corretoria-naming-standards` precisa incorporar
  a tabela da seção 4 antes do primeiro código.

## 10. Roteiro faseado de implementação

### 10.1 Visão geral

Ordem por dependência real, no espírito das 15 etapas da Fase 1.

| Fase | Entrega | Depende de |
|---|---|---|
| **2.1 — Núcleo de anúncios** | `Listing`, `Property`, `Vehicle`, `ListingPhoto` + enums; infra de storage S3; catálogo público navegável com busca por atributos e por raio | Fase 1 (User/Role) |
| **2.2 — Busca salva e notificações** | `SavedSearch` + critérios por tipo, `Notification`, matching assíncrono, e-mail | 2.1 |
| **2.3 — Negociação** | `Proposal`, `ProposalMessage` | 2.1 (propor sobre anúncio) e 2.2 (corretor propor a partir de busca pública) |
| **2.4 — Social** | `Review`, `ListingFavorite`, `RealtorFollow`, `RealtorPost`, `RealtorPostComment`, `RealtorPostLike` | 2.1 (favoritar) e 2.3 (avaliação exige `Proposal`) |
| **2.5 — Moderação** | `ListingReport` | 2.1 (pode ser antecipada logo após 2.1 se preferir) |

### 10.2 Sprints detalhadas

43 sprints, uma por camada dentro de cada cluster, no mesmo padrão arquitetural da Fase 1
(entidade → migration → repository → VO/mapper → service → controller → testes →
documentação), sem estimativa de tempo — só ordem de dependência. Um sprint extra de
infraestrutura entra só nos clusters que introduzem dependência nova (storage no A, e-mail no
B). Um único gate final fecha a fase (mesmo espírito da Etapa 15), não um gate por cluster.

**Cluster A — Núcleo de anúncios**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.1.1 | Infraestrutura de storage: MinIO no docker-compose, SDK S3 no `pom.xml`, propriedades `corretoria.storage.*`, `StorageService` (interface fina) | Fase 1 |
| 2.1.2 | Entidades `Listing`, `Property`, `Vehicle`, `ListingPhoto` + enums (`ListingType`, `OperationType`, `ListingStatus`, `PropertyType`, `PropertyAmenity`, `VehicleType`, `FuelType`, `TransmissionType`, `VehicleCondition`) | 2.1.1 |
| 2.1.3 | Migrations Flyway: `listings`, `properties`, `vehicles`, `listing_photos`, `property_amenities` + CHECKs (`operationType`×`type`, `price`>0) | 2.1.2 |
| 2.1.4 | `ListingRepository` (query methods/`@Query` para filtros e busca por raio) | 2.1.3 |
| 2.1.5 | VOs (`ListingCreationRequest`/`UpdateRequest`/`Response` + `Property`/`Vehicle` embutidos) e mappers | 2.1.3 |
| 2.1.6 | `ListingService` (publicar, buscar/filtrar, atualizar, mudar status, upload/remoção de foto via `StorageService`) + exceções novas | 2.1.4, 2.1.5 |
| 2.1.7 | `ListingController` + ajuste do `SecurityConfig` (catálogo de leitura público) | 2.1.6 |
| 2.1.8 | Testes (unitários de service/mapper/VO + slice de controller: publicação, busca pública, posse, CHECK `Vehicle`×`operationType`) | 2.1.7 |
| 2.1.9 | Documentação OpenAPI + atualização de Postman/README com o catálogo de anúncios | 2.1.8 |

**Cluster B — Busca salva e notificações**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.2.1 | Infraestrutura de e-mail: `spring-boot-starter-mail`, Mailpit no docker-compose, propriedades `corretoria.mail.*` | 2.1 completo |
| 2.2.2 | Entidades `SavedSearch`, `PropertySearchCriteria`, `VehicleSearchCriteria`, `Notification` + enums (`SearchVisibility`, `NotificationType`) | 2.2.1 |
| 2.2.3 | Migrations: `saved_searches`, `property_search_criteria`, `vehicle_search_criteria`, `notifications` | 2.2.2 |
| 2.2.4 | `SavedSearchRepository`, `NotificationRepository` (+ query de buscas ativas compatíveis) | 2.2.3 |
| 2.2.5 | VOs (`SavedSearchCreationRequest`/`Response`, `NotificationResponse`) e mappers | 2.2.3 |
| 2.2.6 | `SavedSearchService`, `NotificationService`, `MatchingService` (evento `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` disparado por `Listing` publicado) | 2.2.4, 2.2.5, 2.1 completo |
| 2.2.7 | `SavedSearchController` (+ endpoint público de buscas `PUBLIC`), `NotificationController` | 2.2.6 |
| 2.2.8 | Testes (matching por tipo/preço/raio, slice de controller) | 2.2.7 |
| 2.2.9 | Documentação OpenAPI + Postman/README com busca salva e notificações | 2.2.8 |

**Cluster C — Negociação**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.3.1 | Entidades `Proposal`, `ProposalMessage` + enums (`ProposalInitiator`, `ProposalStatus`) | 2.1 completo |
| 2.3.2 | Migrations: `proposals`, `proposal_messages` | 2.3.1 |
| 2.3.3 | `ProposalRepository` | 2.3.2 |
| 2.3.4 | VOs (`ProposalCreationRequest`/`StatusUpdateRequest`/`Response`, `ProposalMessageCreationRequest`/`Response`) e mappers | 2.3.2 |
| 2.3.5 | `ProposalService` (criação por cliente ou corretor, mudança de status, mensagens, invariante corretor≠dono) + integração com `NotificationService` | 2.3.3, 2.3.4, 2.2 completo |
| 2.3.6 | `ProposalController` | 2.3.5 |
| 2.3.7 | Testes (as duas direções de iniciação, invariante de posse) | 2.3.6 |
| 2.3.8 | Documentação OpenAPI + Postman/README com negociação | 2.3.7 |

**Cluster D — Social**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.4.1 | Entidades `Review`, `ListingFavorite`, `RealtorFollow`, `RealtorPost`, `RealtorPostComment`, `RealtorPostLike` | 2.3 completo (Review exige Proposal) |
| 2.4.2 | Migrations correspondentes às 6 entidades | 2.4.1 |
| 2.4.3 | Repositories correspondentes | 2.4.2 |
| 2.4.4 | VOs (`Review*`, `RealtorPost*`, `RealtorPostComment*` — favoritar/seguir/curtir são toggle, sem VO de corpo) e mappers | 2.4.2 |
| 2.4.5 | Services (`ReviewService` com elegibilidade via `Proposal`, `ListingFavoriteService`, `RealtorFollowService`, `RealtorPostService` + comentário/curtida) | 2.4.3, 2.4.4 |
| 2.4.6 | Controllers (review, favorito, seguir, mural, comentário, curtida) | 2.4.5 |
| 2.4.7 | Testes | 2.4.6 |
| 2.4.8 | Documentação OpenAPI + Postman/README com a camada social | 2.4.7 |

**Cluster E — Moderação**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.5.1 | Entidade `ListingReport` + enums (`ReportReason`, `ReportStatus`) | 2.1 completo |
| 2.5.2 | Migration `listing_reports` | 2.5.1 |
| 2.5.3 | `ListingReportRepository` | 2.5.2 |
| 2.5.4 | VOs (`ListingReportCreationRequest`/`Response`/`StatusUpdateRequest`) e mapper | 2.5.2 |
| 2.5.5 | `ListingReportService` (abrir denúncia, decidir status, restrito a admin) | 2.5.3, 2.5.4 |
| 2.5.6 | `ListingReportController` | 2.5.5 |
| 2.5.7 | Testes | 2.5.6 |
| 2.5.8 | Documentação OpenAPI + Postman/README com denúncias | 2.5.7 |

**Fechamento**

| Sprint | Entrega | Depende de |
|---|---|---|
| 2.F | Revisão, matriz de evidências e gate final da Fase 2 (mesmo espírito da Etapa 15: `mvn clean verify` verde com a contagem de regras de ArchUnit atualizada, evidência por par endpoint/retorno cobrindo os 5 clusters) — insumo para um futuro `relatorio-corretoria-v2_0.md`, escrito depois da execução | 2.1 a 2.5 completos |

**Nota de ordem:** clusters C, D e E podem correr em paralelo entre si depois que suas
dependências específicas forem satisfeitas (C depende de A+B, D depende de A+C, E depende só
de A) — a numeração 2.1→2.5 é a ordem de menor risco, não uma obrigação estritamente serial.

## 11. Decisões e justificativas (registro consolidado)

Resultado das 5 rodadas de perguntas conduzidas para fechar este
documento — mantido como registro do "porquê", no mesmo espírito da seção
de decisões técnicas do `relatorio-corretoria-v1_0.md`.

| # | Decisão | Descartado |
|---|---|---|
| 1 | Anúncio unificado: `Listing` central associado a `Property` ou `Vehicle` | dois domínios paralelos; atributos dinâmicos (EAV) |
| 2 | Só corretor oferta; cliente só busca | FSBO; híbrido cliente+corretor vinculado |
| 3 | Busca salva com alerta privado **e** opção de anúncio de procura público, no mesmo mecanismo (`SavedSearch.visibility`) | duas entidades separadas |
| 4 | Lead generation + propostas estruturadas, sem fechamento de transação | só lead generation; ciclo completo com histórico/comissão |
| 5 | Social: reputação + favoritos/seguir + feed do corretor | chat livre |
| 6 | Proposta bidirecional (cliente sobre anúncio; corretor a partir de busca pública) | só cliente propõe; só corretor formaliza valor |
| 7 | Fotos via upload multipart para object storage S3-compatible | só URLs; upload para disco/volume local |
| 8 | Moderação por publicação imediata + denúncia | fila de aprovação obrigatória; sinalização automática de risco |
| 9 | Localização: endereço estruturado (bairro/cidade/UF) + lat/long | só endereço estruturado; endereço completo (rua/número) público |
| 10 | Avaliação liberada só após `Proposal` entre as partes | só após proposta aceita; aberta sem interação prévia |
| 11 | Comunicação via thread de mensagens por `Proposal` | só dado de contato; sem texto livre nenhum |
| 12 | Monetização (destaque pago, planos) fora de escopo nesta fase | destaque simples incluído; planos de assinatura incluídos |
| 13 | Aluguel só para imóvel; veículo sempre venda | ambos com venda e aluguel; só venda para os dois |
| 14 | Status do anúncio: `ACTIVE`/`PAUSED`/`REMOVED`/`COMPLETED` | sem `COMPLETED`; só `ACTIVE`/`REMOVED` |
| 15 | Notificação in-app + e-mail | só in-app; sem entidade de notificação |
| 16 | Catálogo público; ações exigem login | tudo autenticado (como hoje); público com dados limitados |
| 17 | Feed do corretor com curtidas e comentários | só publicação simples; só com curtidas |
| 18 | Documento organizado como roteiro faseado | modelo único sem fases |
| 19 | `Property`/`Vehicle` dependem do `Listing` (padrão de `Address`↔`User`) | FK dupla opcional no `Listing`; herança JPA |
| 20 | `Proposal` com 4 estados simples (sem `COUNTERED` formal) | 5 estados com contraproposta formal registrada |

## 12. Suposições e pontos em aberto para revisão

Decisões tomadas sem rodada de pergunta dedicada — sinalizadas para
confirmação ou ajuste antes da Fase 2.1 começar:

1. Entidade nomeada `SavedSearch` (não "SearchRequest") para evitar
   colisão com o sufixo `Request` reservado a VOs — ver nota da seção 4.
2. Placa/chassi do veículo **não** incluído no modelo (exposição pública
   seria risco de privacidade); pode virar campo interno, não exposto em
   `Response`, se necessário para antifraude no futuro.
3. Moeda única assumida (BRL implícito, sem campo de moeda) — coerente com
   CEP/UF já brasileiros no restante do projeto.
4. `ListingFavorite`/`RealtorFollow`/`RealtorPostLike` como junções puras
   (sem `Auditable` completo, só `createdAt` simples, mesmo padrão de
   `user_roles`), mas **com** repository próprio por serem consultadas
   diretamente.
5. Curtir/comentar post liberado a cliente **e** corretor; favoritar
   anúncio/seguir corretor restrito a `ROLE_CUSTOMER`.
6. `Review` é um upsert por par corretor/cliente (`UNIQUE`), não um
   histórico de avaliações.
7. Elegibilidade de `Review` validada por existência de `Proposal` no
   momento da criação, sem guardar qual proposta especificamente
   habilitou.

## Referências

- [`docs/relatorios/relatorio-corretoria-v1_0.md`](../relatorios/relatorio-corretoria-v1_0.md) — relatório técnico completo da Fase 1 (execução, defeitos, evidências).
- [`docs/requisitos/requisitos-corretoria-v1_0.md`](requisitos-corretoria-v1_0.md) — requisitos da Fase 1, no mesmo formato deste documento.
- `.claude/skills/corretoria-naming-standards/SKILL.md` — glossário e
  convenções de nome (a estender com a seção 4 deste documento).
- `.claude/skills/corretoria-architecture-guard/SKILL.md` — regras de
  camada e ArchUnit (sem alteração necessária para a Fase 2).
- `.claude/skills/corretoria-java-clean-solid/SKILL.md` — padrão de
  código Java a seguir na implementação.
