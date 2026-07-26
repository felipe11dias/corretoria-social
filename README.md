# Corretoria

Plataforma que reúne **todos os corretores em um só lugar**. Em vez de cada corretor manter
sua própria operação isolada, a aplicação centraliza o cadastro dos profissionais, dos
clientes e a administração de todos eles sob uma identidade e um controle de acesso únicos.

A entrega é a **plataforma de identidade da Corretoria**: autocadastro, autenticação,
autorização, gestão de usuários pelo administrador, auditoria de escrita, versionamento de
schema e uma API REST versionada e documentada.

> O relatório técnico completo — decisões, justificativas e histórico de cada etapa — está em
> [`relatorio-corretoria-v1_0.md`](relatorio-corretoria-v1_0.md).

---

## Papéis

| Papel | Constante | O que pode fazer |
|-------|-----------|------------------|
| **Corretor** | `ROLE_REALTOR` | Autocadastra-se, autentica-se e gerencia o próprio cadastro |
| **Cliente** | `ROLE_CUSTOMER` | Autocadastra-se, autentica-se e gerencia o próprio cadastro |
| **Administrador** | `ROLE_ADMIN` | Consulta, busca, lista, atualiza e remove **qualquer** usuário |

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

---

## Stack

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 (LTS) | Linguagem |
| Spring Boot | 3.5.16 | Framework base |
| Spring Web / Data JPA / Validation / Security / HATEOAS | gerenciado | API, persistência, validação, segurança e links |
| PostgreSQL | 16 | Banco relacional |
| Flyway | gerenciado | Versionamento de schema |
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

# 2. Suba apenas o banco — útil durante o desenvolvimento
docker compose up -d db

# 3. Suba aplicação + banco juntos
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

O serviço `db` declara um *healthcheck*; a aplicação só sobe depois que o PostgreSQL aceita
conexão, para o Flyway não tentar migrar contra um banco que ainda está iniciando.

### Rodando a aplicação fora do Docker

Com o banco no ar (`docker compose up -d db`):

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

O campo **`type`** é o que distingue erros que compartilham o mesmo status — há três casos de
`400` e dois de `403`:

| `type` | Status | Quando |
|--------|-------:|--------|
| `/problems/validation-error` | 400 | Bean Validation falhou (traz o mapa `errors` por campo) |
| `/problems/invalid-field-format` | 400 | Invariante de VO violada |
| `/problems/invalid-password` | 400 | Senha atual incorreta ou confirmação divergente |
| `/problems/authentication-failed` | 401 | Credenciais inválidas |
| `/problems/missing-token` | 401 | Token ausente, expirado ou inválido |
| `/problems/forbidden-operation` | 403 | Operação proibida em si (ex.: autocadastro pedindo `ROLE_ADMIN`) |
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

O cadeado aparece apenas nos cinco endpoints que exigem token — login e autocadastro são
públicos.

---

## Coleção Postman

[`postman/Corretoria.postman_collection.json`](postman/Corretoria.postman_collection.json)
(formato v2.1), com 23 requisições em duas pastas.

**Como usar:** importe no Postman e execute **de cima para baixo**, ou rode a coleção inteira
no *Runner*. Os scripts de teste populam `{{adminToken}}`, `{{userId}}` e `{{token}}`
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
  são evidências de *efeito*: um `204` prova que a requisição foi aceita, não que o dado mudou.

---

## Testes

```bash
./mvnw test
```

136 testes. A suíte cobre:

| Frente | O que garante |
|--------|---------------|
| `ArchitectureTest` (ArchUnit) | regra de dependência entre camadas, contrato, nomenclatura, ausência de ciclos |
| `UserServiceTest` | regras de negócio do cadastro |
| `UserControllerTest` | autorização por posse e escopo administrativo |
| `GlobalExceptionHandlerTest` | pares status/`type` do contrato de erro |
| `JwtServiceTest`, `UserSecurityTest`, `AuthenticationServiceTest` | token e autorização |
| `EmailTest`, `ZipCodeTest`, `TextUtilsTest`, `ObjectUtilsTest` | invariantes e normalização |

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
│   ├── config/              # SecurityConfig, OpenApiConfig, ClockConfig, JpaAuditingConfig
│   ├── controller/          # AuthController, UserController, UserModelAssembler
│   ├── entity/              # Auditable, User, Role, Address, PasswordResetToken
│   ├── enums/               # RoleName
│   ├── exception/           # exceções de domínio, ProblemType, GlobalExceptionHandler
│   ├── mapper/              # UserMapper, AddressMapper (MapStruct)
│   ├── repository/          # Spring Data JPA
│   ├── security/            # JWT, UserSecurity, auditoria de autoria
│   ├── service/             # UserService, AuthenticationService
│   ├── util/                # TextUtils, ObjectUtils
│   └── vo/                  # shared (Email, ZipCode) e v1 (request/response)
├── src/main/resources/db/migration/   # V1 a V5 (Flyway)
└── src/test/java/com/corretoria/
```

---

**Autor:** Felipe Dias Mac Dowell
