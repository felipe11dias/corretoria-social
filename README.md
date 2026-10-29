# Corretoria

Plataforma que reúne **todos os corretores em um só lugar**. Em vez de cada corretor manter
sua própria operação isolada, a aplicação centraliza o cadastro dos profissionais, dos
clientes e a administração de todos eles sob uma identidade e um controle de acesso únicos.

A **Fase 1** entregou a plataforma de identidade: autocadastro, autenticação, autorização,
gestão de usuários pelo administrador, auditoria de escrita, versionamento de schema e uma API
REST versionada e documentada.

A **Fase 2** está em execução e constrói o domínio sobre essa fundação:

- **Cluster A — núcleo de anúncios:** corretores publicam imóveis e automóveis com fotos, e o
  catálogo é navegável **sem token**, com filtros combináveis e busca por raio.
- **Cluster B — busca salva e notificações:** clientes salvam critérios de procura e são
  avisados quando um anúncio compatível é publicado; a procura pode ser pública, virando um
  **anúncio de procura** que os corretores consultam — o mercado reverso da plataforma.
- **Cluster C — negociação:** propostas estruturadas nas duas direções, com thread de mensagens
  por proposta — o cliente propõe sobre um anúncio, o corretor propõe a partir de uma procura
  pública.
- **Cluster D — social:** reputação do corretor condicionada a negociação real, favoritos,
  acompanhar corretor e mural com comentários e curtidas.
- **Cluster E — moderação:** denúncia de anúncio aberta a qualquer usuário autenticado e fila
  de decisão exclusiva do administrador — o contrapeso da publicação imediata.

> Relatório técnico da Fase 1 (decisões, justificativas e histórico de cada etapa):
> [`relatorio-corretoria-v1_0.md`](docs/relatorios/relatorio-corretoria-v1_0.md).
> Requisitos e roteiro da Fase 2:
> [`requisitos-corretoria-v2_0.md`](docs/requisitos/requisitos-corretoria-v2_0.md).

---

## Papéis

| Papel | Constante | O que pode fazer |
|-------|-----------|------------------|
| **Corretor** | `ROLE_REALTOR` | Gerencia o próprio cadastro e **publica anúncios** de imóveis e automóveis |
| **Cliente** | `ROLE_CUSTOMER` | Gerencia o próprio cadastro e **busca** no catálogo; nunca oferta |
| **Administrador** | `ROLE_ADMIN` | Consulta, busca, lista, atualiza e remove **qualquer** usuário ou anúncio, e decide as denúncias |
| **Visitante** | — | Lista e abre anúncios, lê o mural e a reputação dos corretores, sem token |

Um mesmo usuário pode acumular papéis — eles vivem em tabela associativa, não em coluna enum.

**`ROLE_ADMIN` não é obtido por autocadastro.** O endpoint público recusa a solicitação com
`403`; o administrador nasce por seed (migration `V3`) ou por fluxo autenticado.

### Matriz de permissões

| Operação | `ROLE_REALTOR` | `ROLE_CUSTOMER` | `ROLE_ADMIN` |
|----------|:--------------:|:---------------:|:------------:|
| Autocadastro público | ✅ | ✅ | ❌ |
| Autenticar | ✅ | ✅ | ✅ |
| Consultar/atualizar **o próprio** cadastro | ✅ | ✅ | ✅ |
| Trocar a própria senha | ✅ | ✅ | ✅ |
| Excluir o próprio cadastro | ✅ | ✅ | ✅ |
| Consultar cadastro de **outro** usuário | ❌ | ❌ | ✅ |
| Listar / buscar usuários | ❌ | ❌ | ✅ |
| Atualizar / excluir qualquer usuário | ❌ | ❌ | ✅ |
| Listar / abrir anúncio (também sem token) | ✅ | ✅ | ✅ |
| Publicar anúncio | ✅ | ❌ | ❌ |
| Editar / pausar / fotografar **o próprio** anúncio | ✅ | — | ✅ (qualquer) |
| Criar busca salva (privada ou pública) | ❌ | ✅ | ❌ |
| Ver a vitrine de anúncios de procura | ✅ | ✅ | ✅ |
| Editar / excluir **a própria** busca salva | — | ✅ | ✅ (qualquer) |
| Ler / marcar como lida **a própria** notificação | ✅ | ✅ | ✅ (só as suas) |
| Propor sobre um anúncio | ❌ | ✅ | ❌ |
| Propor a partir de uma procura pública | ✅ (só o próprio anúncio) | ❌ | ❌ |
| Aceitar, recusar ou retirar proposta | ✅ (a que participa) | ✅ (a que participa) | ❌ |
| Escrever no thread da proposta | ✅ (a que participa) | ✅ (a que participa) | ❌ |
| Favoritar anúncio / acompanhar corretor | ❌ | ✅ | ❌ |
| Avaliar corretor (após negociar) | ❌ | ✅ | ❌ |
| Publicar no **próprio** mural | ✅ | ❌ | ❌ |
| Comentar / curtir publicação | ✅ | ✅ | ✅ |
| Ler mural e reputação (também sem token) | ✅ | ✅ | ✅ |
| Denunciar anúncio | ✅ | ✅ | ✅ |
| Ver a fila de moderação e decidir denúncia | ❌ | ❌ | ✅ |

---

## Stack

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 (LTS) | Linguagem |
| Spring Boot | 3.5.16 | Framework base |
| Spring Web / Data JPA / Validation / Security / HATEOAS | gerenciado | API, persistência, validação, segurança e links |
| PostgreSQL | 16 | Banco relacional |
| Flyway | gerenciado | Versionamento de schema |
| AWS SDK for Java (S3) | 2.42.14 | Cliente de object storage das fotos de anúncio |
| MinIO | S3-compatible | Storage local em desenvolvimento (produção: S3) |
| Spring Mail | gerenciado | Notificação por e-mail |
| Mailpit | 1.21 | SMTP local em desenvolvimento, com caixa de entrada web |
| springdoc-openapi | 2.8.17 | Swagger |
| jjwt | 0.12.6 | JWT |
| MapStruct | 1.6.3 | Mapeamento VO ↔ entidade |
| Lombok | gerenciado | Redução de boilerplate |
| JUnit 5 + Mockito + AssertJ | gerenciado | Testes |
| ArchUnit | 1.4.1 | Testes de arquitetura |

**Arquitetura em camadas:** `controller` → `service` → `repository` → `entity`, com `vo`,
`mapper`, `exception`, `security`, `config` e `util` como componentes transversais.

---

## Pré-requisitos

- **Docker** e **Docker Compose** — para subir o banco e a aplicação.
- **JDK 21** com `JAVA_HOME` apontado para ele — apenas se for rodar fora do Docker.

Não é preciso instalar Maven: o repositório traz o **Maven Wrapper**.

---

## Execução com Docker Compose

```bash
# 1. (Opcional) crie seu .env a partir do exemplo
cp .env.example .env

# 2. Suba a infraestrutura — útil durante o desenvolvimento
docker compose up -d db storage storage-init mail

# 3. Suba tudo junto
docker compose up --build

# 4. Encerre
docker compose down

# 4b. Encerre e apague os dados do banco
docker compose down -v
```

Disponível em:

- API — `http://localhost:8080`
- Swagger UI — `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON — `http://localhost:8080/v3/api-docs`
- Health — `http://localhost:8080/actuator/health`
- Storage (S3) — `http://localhost:9000`
- Console do storage — `http://localhost:9001` (usuário e senha: `minioadmin`)
- Caixa de entrada de desenvolvimento — `http://localhost:8025`

Os serviços `db` e `storage` declaram *healthcheck*; a aplicação só sobe depois que os dois
respondem, para o Flyway não tentar migrar contra um banco que ainda está iniciando.

O serviço `storage-init` roda uma vez e encerra: cria o bucket e libera **leitura anônima**
nele. A foto acompanha o anúncio, e o catálogo é público — exigir token para ver a imagem de
um anúncio que qualquer visitante já pode abrir não protegeria nada.

### Rodando a aplicação fora do Docker

Com a infraestrutura no ar (`docker compose up -d db storage storage-init mail`):

```bash
./mvnw spring-boot:run
```

---

## Variáveis de ambiente

Todas têm valor padrão; veja [`.env.example`](.env.example).

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_NAME` | `corretoria-app` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `DB_HOST` | `localhost` (local) / `db` (compose) | Host do banco |
| `DB_PORT` | `5432` | Porta do banco |
| `JWT_SECRET` | *(valor de exemplo)* | Segredo HMAC-SHA256, **mínimo 32 caracteres** |
| `JWT_EXPIRATION` | `3600000` | Validade do token, em ms |
| `STORAGE_ENDPOINT` | `http://localhost:9000` (local) / `http://storage:9000` (compose) | Endereço do storage **usado pela aplicação** |
| `STORAGE_PUBLIC_BASE_URL` | `http://localhost:9000` | Endereço **visto do navegador**, base das URLs de foto |
| `STORAGE_REGION` | `us-east-1` | Região informada ao SDK |
| `STORAGE_BUCKET` | `corretoria-listing-photos` | Bucket das fotos de anúncio |
| `STORAGE_ACCESS_KEY` | `minioadmin` | Credencial de acesso |
| `STORAGE_SECRET_KEY` | `minioadmin` | Credencial secreta |
| `STORAGE_PORT` / `STORAGE_CONSOLE_PORT` | `9000` / `9001` | Portas publicadas pelo MinIO |
| `MAIL_HOST` | `localhost` (local) / `mail` (compose) | Host do SMTP |
| `MAIL_PORT` | `1025` | Porta do SMTP |
| `MAIL_WEB_PORT` | `8025` | Caixa de entrada web do Mailpit |
| `MAIL_FROM` | `nao-responda@corretoria.app` | Remetente das notificações |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vazios | Credenciais do SMTP real |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | `false` | Autenticação e TLS do SMTP real |

São **dois endereços de propósito**: dentro do compose a aplicação fala com o storage pelo nome
do serviço (`storage`), enquanto o link devolvido na API é aberto pelo navegador, fora dessa
rede (`localhost`).

O tamanho do `JWT_SECRET` é validado na **inicialização**: um segredo curto derruba a
aplicação na subida, e não no primeiro login.

---

## Autenticação

A API é **stateless**: não há sessão no servidor. Cada requisição protegida leva o token no
cabeçalho `Authorization`.

```bash
# 1. Obtenha o token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin.demo","password":"admin12345"}'

# 2. Use-o nas demais chamadas
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <token>"
```

### Usuários de demonstração

Criados pelas migrations `V2` e `V3`. **São apenas para teste — remova antes de um ambiente
real.**

| Login | Senha | Papel |
|-------|-------|-------|
| `corretor.demo` | `corretor12345` | `ROLE_REALTOR` |
| `cliente.demo` | `cliente12345` | `ROLE_CUSTOMER` |
| `admin.demo` | `admin12345` | `ROLE_ADMIN` |

---

## Endpoints

### Autenticação

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/auth/login` | Valida credenciais e emite o token | `200` | Pública |

### Usuários

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/users` | Autocadastro de corretor ou cliente | `201` + `Location` | Pública |
| `GET` | `/api/v1/users/{id}` | Consulta por id | `200` | Dono ou `ROLE_ADMIN` |
| `GET` | `/api/v1/users` | Lista paginada | `200` | `ROLE_ADMIN` |
| `GET` | `/api/v1/users?name=...` | Busca paginada por nome | `200` | `ROLE_ADMIN` |
| `PUT` | `/api/v1/users/{id}` | Atualiza **nome e endereços** | `200` | Dono ou `ROLE_ADMIN` |
| `PATCH` | `/api/v1/users/{id}/password` | Troca de senha | `204` | Dono ou `ROLE_ADMIN` |
| `DELETE` | `/api/v1/users/{id}` | Exclui | `204` | Dono ou `ROLE_ADMIN` |

**E-mail, login, senha e papéis são imutáveis pelo `PUT`.** A senha tem endpoint próprio; os
papéis são concessão do administrador.

**Paginação:** `page` (base 0), `size` (padrão 20) e `sort` (padrão `name`) — por exemplo
`?page=0&size=10&sort=name,asc`. As respostas trazem links HATEOAS (`self`, `users`, e
`first`/`prev`/`next`/`last` no envelope paginado).

### Anúncios

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/listings` | Publica um anúncio | `201` + `Location` | `ROLE_REALTOR` |
| `GET` | `/api/v1/listings` | Busca paginada no catálogo | `200` | **Pública** |
| `GET` | `/api/v1/listings/{id}` | Consulta por id | `200` | **Pública** |
| `PUT` | `/api/v1/listings/{id}` | Atualiza conteúdo e detalhamento | `200` | Dono ou `ROLE_ADMIN` |
| `PATCH` | `/api/v1/listings/{id}/status` | Muda a situação | `200` | Dono ou `ROLE_ADMIN` |
| `POST` | `/api/v1/listings/{id}/photos` | Envia uma foto (`multipart`, parte `photo`) | `201` | Dono ou `ROLE_ADMIN` |
| `DELETE` | `/api/v1/listings/{id}/photos/{photoId}` | Remove uma foto | `204` | Dono ou `ROLE_ADMIN` |

Um anúncio é de **imóvel** (`type=PROPERTY`, com o objeto `property`) **ou** de **automóvel**
(`type=VEHICLE`, com `vehicle`) — nunca dos dois. O **tipo é imutável** depois de publicado, e
**automóvel só admite `SALE`**: alugar veículo é operação de locadora, não de corretor. As duas
regras valem também no banco, como `CHECK`.

O **corretor dono nunca vem no corpo** da requisição: é resolvido a partir do token.

**Filtros da busca** (todos opcionais e combináveis):

| Grupo | Parâmetros |
|-------|------------|
| Comuns | `type`, `operationType`, `status`, `realtorId`, `city`, `state`, `neighborhood`, `minPrice`, `maxPrice` |
| Imóvel | `propertyType`, `minBedrooms`, `minTotalArea` |
| Automóvel | `vehicleType`, `brand`, `maxMileageKm`, `minManufactureYear` |
| Raio | `latitude`, `longitude`, `radiusKm` (os três juntos) |

```bash
# Apartamentos de até R$ 500 mil em Fortaleza, num raio de 5 km da Aldeota
curl "http://localhost:8080/api/v1/listings?type=PROPERTY&city=Fortaleza\
&maxPrice=500000&latitude=-3.7327&longitude=-38.5270&radiusKm=5&sort=price,asc"
```

Sem `status`, a busca devolve **apenas anúncios `ACTIVE`** — é o catálogo. Consultar outra
situação exige ser o corretor dono (com `realtorId`) ou `ROLE_ADMIN`: pausar tira o anúncio da
vitrine, e deixar qualquer visitante listá-lo pelo filtro anularia essa decisão.

**Fotos:** `image/jpeg`, `image/png` ou `image/webp`, até 5 MB, no máximo 20 por anúncio. A
primeira vira a capa; remover a capa promove a próxima. O banco guarda a **chave do objeto**, e
a resposta devolve a **URL montada** — gravar a URL invalidaria as linhas na primeira mudança
de infraestrutura.

**Paginação:** padrão `sort=createdAt,desc` — o catálogo abre pelo anúncio mais recente.

### Buscas salvas

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/saved-searches` | Cria uma busca salva | `201` + `Location` | `ROLE_CUSTOMER` |
| `GET` | `/api/v1/saved-searches` | Lista as próprias buscas | `200` | `ROLE_CUSTOMER` |
| `GET` | `/api/v1/saved-searches/public` | Vitrine de anúncios de procura | `200` | Autenticado |
| `PUT` | `/api/v1/saved-searches/{id}` | Atualiza critérios, título, visibilidade e o estado ativo | `200` | Dono ou `ROLE_ADMIN` |
| `DELETE` | `/api/v1/saved-searches/{id}` | Exclui | `204` | Dono ou `ROLE_ADMIN` |

Uma busca salva é o **mesmo mecanismo** em dois usos, separados por `visibility`:

- `PRIVATE` — alerta silencioso: quando um anúncio compatível é publicado, o dono recebe
  notificação in-app e e-mail;
- `PUBLIC` — faz o mesmo **e** expõe a procura na vitrine que os corretores consultam. É o
  mercado reverso: o corretor também caça cliente.

**Nada aqui é público**, nem a vitrine — uma procura revela o que a pessoa quer, onde e por
quanto. Os critérios seguem a regra do catálogo ao contrário: critério ausente significa
"qualquer valor", e por isso casa. Os critérios específicos (`propertyCriteria` ou
`vehicleCriteria`) devem ser os do tipo da busca — ou nenhum, já que "qualquer imóvel em
Fortaleza" é uma procura legítima. `active: false` pausa o alerta sem apagar os critérios.

### Notificações

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `GET` | `/api/v1/notifications` | Lista as próprias notificações | `200` | Autenticado |
| `PATCH` | `/api/v1/notifications/{id}/read` | Marca como lida | `200` | **Só o destinatário** |

Notificação é correspondência: **não há escopo administrativo** — nem o administrador lê ou
marca a notificação alheia. A resposta da listagem traz o cabeçalho
`X-Corretoria-Unread-Count` com o total pendente.

O aviso in-app é a **fonte de verdade**; o e-mail é entrega adicional, de melhor esforço: um
SMTP fora do ar vira `warn` no log e não faz o usuário perder o registro.

**O casamento é assíncrono.** Publicar um anúncio dispara um evento consumido *depois do
commit*, em outra thread: o corretor recebe o `201` sem esperar pela varredura das buscas
salvas. Em desenvolvimento, o e-mail resultante aparece em `http://localhost:8025`.


### Propostas

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/proposals` | Abre uma negociação | `201` + `Location` | Autenticado |
| `GET` | `/api/v1/proposals` | Lista as próprias negociações | `200` | Autenticado |
| `GET` | `/api/v1/proposals/{id}` | Abre a negociação com o thread | `200` | **Só participantes** |
| `PATCH` | `/api/v1/proposals/{id}/status` | Aceita, recusa ou retira | `200` | **Só participantes** |
| `POST` | `/api/v1/proposals/{id}/messages` | Escreve no thread | `201` | **Só participantes** |

A proposta é **bidirecional**, e a direção **não vem no corpo** — é deduzida de quem está
autenticado:

- **quem chama não é o dono do anúncio** → é o cliente propondo sobre o catálogo (exige
  `ROLE_CUSTOMER`, e `savedSearchId` é proibido);
- **quem chama é o dono do anúncio** → é o corretor abordando um cliente, e `savedSearchId`
  passa a ser **obrigatório**: é a busca pública que diz a quem a proposta se destina e o que
  autoriza a abordagem.

Deduzir em vez de perguntar elimina a possibilidade de alguém se declarar do lado errado da
mesa. Só anúncio `ACTIVE` aceita proposta, e ninguém negocia consigo mesmo — a checagem importa
porque um mesmo usuário pode acumular os dois papéis.

**Quem decide o quê:** aceitar e recusar são da **contraparte**; retirar é de **quem propôs**.
Os três desfechos valem apenas a partir de `PENDING` e são definitivos — retomar é abrir uma
proposta nova. O link `status` some da resposta assim que a proposta é decidida.

**Não existe estado de contraproposta:** contrapor é escrever no thread sugerindo outro valor.
O thread continua aberto depois do desfecho, porque combinar detalhes de uma proposta aceita é
conversa legítima.

Cada passo notifica o outro lado (`NEW_PROPOSAL`, `PROPOSAL_STATUS_CHANGED`,
`NEW_PROPOSAL_MESSAGE`) — in-app e por e-mail, pelo mesmo caminho do Cluster B.

### Camada social

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `PUT` | `/api/v1/realtors/{id}/reviews` | Avalia o corretor | `200` | `ROLE_CUSTOMER` que negociou |
| `GET` | `/api/v1/realtors/{id}/reviews` | Lista as avaliações | `200` | **Pública** |
| `PUT` / `DELETE` | `/api/v1/listings/{id}/favorite` | Marca / desmarca favorito | `200` / `204` | `ROLE_CUSTOMER` |
| `GET` | `/api/v1/favorites` | Lista os próprios favoritos | `200` | `ROLE_CUSTOMER` |
| `PUT` / `DELETE` | `/api/v1/realtors/{id}/follow` | Passa / deixa de acompanhar | `200` / `204` | `ROLE_CUSTOMER` |
| `GET` | `/api/v1/follows` | Lista os corretores que acompanha | `200` | `ROLE_CUSTOMER` |
| `POST` | `/api/v1/realtors/{id}/posts` | Publica no próprio mural | `201` | Dono do mural |
| `GET` | `/api/v1/realtors/{id}/posts` | Lista o mural | `200` | **Pública** |
| `POST` | `/api/v1/posts/{id}/comments` | Comenta uma publicação | `201` | Autenticado |
| `PUT` / `DELETE` | `/api/v1/posts/{id}/like` | Curte / descurte | `200` | Autenticado |

**A avaliação é *gated* por interação real:** só avalia quem já negociou com aquele corretor —
basta a proposta ter existido, sem precisar ter sido aceita, porque um corretor que sumiu
depois de receber a proposta também merece ser avaliado. É o análogo da "compra verificada", e
tem `type` próprio (`/problems/review-not-allowed`) porque não é acesso negado: a porta se abre
sozinha assim que houver negociação.

O verbo é `PUT`, não `POST`: **uma avaliação por par** corretor/cliente, e avaliar de novo
substitui a nota. Não há histórico — o que a plataforma publica é a opinião atual de cada
cliente. O cabeçalho `X-Corretoria-Average-Score` traz a média de **todas** as avaliações, e
não só das que couberam na página.

**Favoritar, acompanhar e curtir são idempotentes** (`PUT`/`DELETE`, sem `POST`): o recurso é a
relação entre quem chama e o que está na rota — existe ou não existe. Repetir a chamada depois
de uma falha de rede não vira `409`, e o `UNIQUE` no banco garante o que a interface promete:
uma curtida por pessoa, e não um contador que sobe a cada clique.

**Quem pode o quê:** favoritar, acompanhar e avaliar são do cliente — são formas de marcar
interesse, e não fazem sentido para quem oferta. Já **comentar e curtir são de qualquer usuário
autenticado**, inclusive de outro corretor: conversa de mural é pública, ao contrário do thread
de uma proposta.

### Moderação

| Método | Rota | Descrição | Sucesso | Autorização |
|--------|------|-----------|---------|-------------|
| `POST` | `/api/v1/listings/{id}/reports` | Denuncia um anúncio | `201` | Autenticado |
| `GET` | `/api/v1/reports` | Fila de moderação | `200` | `ROLE_ADMIN` |
| `PATCH` | `/api/v1/reports/{id}/status` | Decide a denúncia | `200` | `ROLE_ADMIN` |

A denúncia é **o contrapeso da publicação imediata**: a plataforma confia no corretor para
publicar sem fila de aprovação e confia na comunidade para apontar o que estiver errado — sem o
segundo, a confiança do primeiro ficaria sem resposta.

A assimetria é deliberada: **abrir** é de qualquer usuário autenticado, porque o contrapeso só
funciona se denunciar for fácil; **ver a fila e decidir** é exclusivo de `ROLE_ADMIN`, porque
decisão de moderação não é opinião de usuário. Nem o denunciante acompanha a própria denúncia —
a fila é ferramenta de trabalho do moderador.

| Regra | Resposta |
|-------|---------|
| Denunciar o próprio anúncio | `403` — para isso existe `PATCH /listings/{id}/status` |
| Segunda denúncia sua em aberto no mesmo anúncio | `409` — depois de decidida, uma nova é legítima |
| Decidir uma denúncia já decidida | `403` — um problema que reaparece é denúncia nova |
| Devolver a denúncia para `OPEN` | `403` — `OPEN` é de onde ela parte |

O `reason` é de **lista fechada** (`MISLEADING_INFO`, `PROHIBITED_ITEM`, `SUSPECTED_FRAUD`,
`INAPPROPRIATE_CONTENT`, `OTHER`), e é ele que permite priorizar a fila: suspeita de fraude não
espera na mesma ordem que uma foto desatualizada. O texto livre fica em `detail`, como
complemento, nunca como classificação.

Sem `status`, a fila devolve as denúncias `OPEN`, das mais antigas para as mais novas — quem
esperou mais é atendido primeiro. A resposta traz `reportCountForListing`, que distingue uma
reclamação isolada de um padrão, e `decidedBy`, que vem da auditoria e só aparece depois da
decisão.

**Decidir não mexe no anúncio.** Acolher a denúncia registra a decisão; retirar o anúncio do ar
continua sendo uma ação explícita sobre ele, para que um clique na fila não apague o anúncio de
alguém por engano. Quem é avisado da denúncia nova são os **moderadores**, não o corretor
denunciado — contar a ele antes da análise daria a quem agiu de má-fé o tempo de apagar o
rastro.
### Erros

Todas as respostas de erro seguem **ProblemDetail (RFC 7807)**:

```json
{
  "type": "https://api.corretoria.app/problems/duplicate-resource",
  "title": "Conflito de dados",
  "status": 409,
  "detail": "E-mail já cadastrado: joao@email.com",
  "instance": "/api/v1/users",
  "timestamp": "2026-07-25T10:00:00Z"
}
```

O campo **`type`** é o que distingue erros que compartilham o mesmo status — há seis casos de
`400` e três de `403`:

| `type` | Status | Quando |
|--------|-------:|--------|
| `/problems/validation-error` | 400 | Bean Validation falhou (traz o mapa `errors` por campo) |
| `/problems/invalid-field-format` | 400 | Invariante de VO violada, ou critérios de busca incoerentes |
| `/problems/invalid-listing` | 400 | Campos válidos, combinação inexistente no domínio (detalhamento fora do tipo, aluguel de veículo, limite de fotos) |
| `/problems/invalid-saved-search` | 400 | Idem, na busca salva (critérios fora do tipo, raio sem coordenada, faixa de preço invertida) |
| `/problems/invalid-proposal` | 400 | Idem, na negociação (anúncio fora do ar, proposta já decidida, origem incoerente com a direção) |
| `/problems/invalid-password` | 400 | Senha atual incorreta ou confirmação divergente |
| `/problems/authentication-failed` | 401 | Credenciais inválidas |
| `/problems/missing-token` | 401 | Token ausente, expirado ou inválido |
| `/problems/forbidden-operation` | 403 | Operação proibida em si (ex.: autocadastro pedindo `ROLE_ADMIN`) |
| `/problems/review-not-allowed` | 403 | Avaliação sem negociação prévia — a porta se abre sozinha depois da primeira proposta |
| `/problems/access-denied` | 403 | Recurso de outro usuário |
| `/problems/resource-not-found` | 404 | Id inexistente |
| `/problems/duplicate-resource` | 409 | E-mail ou login já cadastrado |
| `/problems/internal-error` | 500 | Erro não previsto |

---

## Swagger

`http://localhost:8080/swagger-ui.html`

1. Chame `POST /api/v1/auth/login` com um usuário de demonstração.
2. Copie o `token` da resposta.
3. Clique em **Authorize**, cole o token e confirme.
4. Execute os endpoints protegidos direto pela interface.

O cadeado aparece apenas nos endpoints que exigem token — login, autocadastro e a leitura do
catálogo de anúncios são públicos.

---

## Coleção Postman

[`postman/Corretoria.postman_collection.json`](postman/Corretoria.postman_collection.json)
(formato v2.1), com 89 requisições em oito pastas: **Autenticação**, **Usuários**,
**Anúncios**, **Buscas salvas**, **Notificações**, **Propostas**, **Social** e **Moderação**.

**Como usar:** importe no Postman e execute **de cima para baixo**, ou rode a coleção inteira
no *Runner*. Os scripts de teste populam `{{adminToken}}`, `{{realtorToken}}`,
`{{customerToken}}`, `{{userId}}`, `{{listingId}}`, `{{savedSearchId}}`, `{{proposalId}}`, `{{postId}}`, `{{reportId}}` e `{{token}}`
automaticamente — nada precisa ser preenchido à mão. Ajuste `{{baseUrl}}` se a API não estiver
em `http://localhost:8080`.

A sequência é **re-executável**: o cadastro gera login e e-mail únicos a partir de `{{runId}}`,
e o usuário criado é excluído no fim.

Além do caminho feliz, a coleção cobre os cenários que provam as regras:

- **e-mail duplicado divergindo só na caixa → `409`**, que só acontece porque o VO `Email`
  normaliza antes de checar unicidade;
- **autocadastro pedindo `ROLE_ADMIN` → `403`**;
- **recurso de outro usuário → `403`** na consulta, na busca e na listagem;
- **login com a senha nova após a troca → `200`** e **consulta ao id excluído → `404`**, que
  são evidências de *efeito*: um `204` prova que a requisição foi aceita, não que o dado mudou;
- **anúncio de veículo com aluguel → `400 invalid-listing`** e **detalhamento fora do tipo →
  `400 invalid-listing`**, casos em que cada campo é válido e a *combinação* é que não existe;
- **catálogo consultado sem token nenhum → `200`**, ao lado de **publicação sem token →
  `401`** e **publicação por cliente → `403`**, que juntos delimitam a abertura do catálogo;
- **anúncio pausado some da vitrine, mas o dono o encontra filtrando `status=PAUSED` com o
  próprio `realtorId`** — os dois lados da mesma regra;
- **busca salva criada por corretor → `403`** e **critérios de veículo em busca de imóvel →
  `400 invalid-saved-search`**;
- **vitrine de procura sem token → `401`**, ao lado do catálogo de anúncios lido sem token
  nenhum: as duas aberturas são deliberadamente diferentes;
- **notificação alheia com token de administrador → `403`**, que fixa a ausência de escopo
  administrativo sobre correspondência.
- **quem propôs tentando aceitar a própria proposta → `403`**, seguido da contraparte aceitando
  → `200`, e da segunda decisão sobre a mesma proposta → `400 invalid-proposal`;
- **thread de proposta aberto com token de administrador → `403`**, que fixa que negociação não
  tem plateia.
- **avaliação do mesmo par substituindo a nota em vez de acumular**, e **curtida repetida que
  não mexe na contagem** — as duas idempotências que o `UNIQUE` sustenta no banco;
- **denúncia do próprio anúncio → `403`**, **segunda denúncia em aberto → `409`** e **fila de
  moderação pedida pelo cliente → `403`**, que juntas delimitam a assimetria da moderação.

A publicação de um anúncio compatível com a busca salva fica na pasta **Buscas salvas** e
dispara o casamento. Como ele é **assíncrono**, a coleção não afirma que a notificação já
chegou — verifique a caixa de entrada em `http://localhost:8025` e a listagem de notificações.

O envio de foto é `multipart` e exige escolher um arquivo local, então fica fora da execução
automática. Pelo terminal:

```bash
curl -H "Authorization: Bearer $TOKEN" -F "photo=@fachada.jpg" \
  http://localhost:8080/api/v1/listings/1/photos
```

---

## Testes

```bash
./mvnw test
```

290 testes. A suíte cobre:

| Frente | O que garante |
|--------|---------------|
| `ArchitectureTest` (ArchUnit) | 35 regras: dependência entre camadas, contrato, nomenclatura, eventos, ausência de ciclos |
| `UserServiceTest` | regras de negócio do cadastro |
| `UserControllerTest` | autorização por posse e escopo administrativo |
| `ListingServiceTest` | regras do anúncio: dono vindo do token, detalhamento coerente com o tipo, veículo sem aluguel, capa e posição das fotos |
| `ListingControllerTest` | fronteira exata do catálogo público: ler é livre, escrever exige token, papel e posse |
| `SavedSearchServiceTest` | só cliente procura, critérios coerentes com o tipo, dependente criado/atualizado/removido |
| `ListingMatchingServiceTest` | notifica cada dono de busca compatível e para quando o anúncio já não existe |
| `NotificationServiceTest` | aviso in-app sobrevive à falha de SMTP |
| `SavedSearchControllerTest`, `NotificationControllerTest` | nada na busca salva é público; notificação não tem supervisor |
| `ProposalServiceTest` | as duas direções de iniciação, invariantes da negociação e quem pode cada desfecho |
| `ProposalControllerTest` | thread fechado aos dois participantes, sem escopo administrativo |
| `ReviewServiceTest` | elegibilidade por negociação prévia e o upsert de uma avaliação por par |
| `ListingFavoriteServiceTest`, `RealtorFollowServiceTest` | idempotência dos vínculos e aviso só no nascimento |
| `RealtorPostServiceTest` | curtida que não duplica e comentário com autor fechado |
| `RealtorPostControllerTest` | as três fronteiras do mural: ler é público, publicar é do dono, interagir é de qualquer autenticado |
| `ListingReportServiceTest` | abrir é fácil e decidir é restrito: dono barrado, segunda denúncia em aberto barrada, decisão definitiva |
| `ListingReportControllerTest` | a assimetria da moderação: qualquer autenticado denuncia, só o administrador vê a fila e decide |
| `GlobalExceptionHandlerTest` | pares status/`type` do contrato de erro |
| `JwtServiceTest`, `UserSecurityTest`, `ListingSecurityTest`, `AuthenticationServiceTest` | token e autorização por posse |
| `EmailTest`, `ZipCodeTest`, `PhotoContentTest`, `TextUtilsTest`, `ObjectUtilsTest` | invariantes e normalização |

> **Confira a contagem, não só o `BUILD SUCCESS`.** O Surefire 3.5.3 descarta silenciosamente
> os testes da engine do ArchUnit — o build fica verde com `Tests run: 0` e nenhuma regra de
> arquitetura é checada. Por isso o `pom.xml` fixa o Surefire em **3.5.2**.

---

## Governança por skills

Três skills versionadas em [`.claude/skills/`](.claude/skills) mantêm a integridade do projeto
ao longo do tempo. São acionadas **antes** de qualquer código Java ser escrito.

| Skill | Responsabilidade |
|-------|------------------|
| `corretoria-naming-standards` | Padrão único de nomenclatura: Java, banco, API, JSON, Flyway, Git, testes e Docker |
| `corretoria-architecture-guard` | Regras da arquitetura em camadas e seu enforcement com ArchUnit |
| `corretoria-java-clean-solid` | Java 21 idiomático, clean code e SOLID |

A skill de arquitetura tem **poder de veto**: mudança que viole a regra de dependência é
recusada antes de o código existir. As regras não são só documentação — `ArchitectureTest`
falha o build quando alguma é violada, inclusive por código gerado.

---

## Estrutura do projeto

```
corretoria/
├── .claude/skills/          # as três skills de governança
├── postman/                 # coleção da API
├── src/main/java/com/corretoria/
│   ├── config/              # SecurityConfig, StorageConfig, MailConfig, AsyncConfig, OpenApiConfig, ClockConfig, JpaAuditingConfig
│   ├── controller/          # um controller por agregado + assemblers HATEOAS
│   ├── entity/              # identidade, anúncio, busca salva, notificação, negociação e camada social (21 classes, incluindo Auditable)
│   ├── enums/               # RoleName e os enums do anúncio, da busca salva, da notificação e da proposta
│   ├── event/               # ListingPublishedEvent — fatos de domínio, sempre records com id
│   ├── exception/           # exceções de domínio, ProblemType, GlobalExceptionHandler
│   ├── mapper/              # um mapper por agregado (MapStruct)
│   ├── repository/          # Spring Data JPA
│   ├── security/            # JWT e um bean de posse por agregado (UserSecurity, ListingSecurity, SavedSearchSecurity, NotificationSecurity, ProposalSecurity, RealtorPostSecurity)
│   ├── service/             # regras de negócio + portas de infraestrutura (StorageService, MailService)
│   ├── util/                # TextUtils, ObjectUtils
│   └── vo/                  # shared (Email, ZipCode, PhotoContent) e v1 (request/response)
├── src/main/resources/db/migration/   # V1 a V10 (Flyway)
└── src/test/java/com/corretoria/
```

---

**Autor:** Felipe Dias Mac Dowell
