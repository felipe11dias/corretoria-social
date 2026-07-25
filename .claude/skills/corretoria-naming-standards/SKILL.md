---
name: corretoria-naming-standards
description: >
  Guardião do padrão de nomenclatura do projeto Corretoria (Java 21 · Spring Boot 3.5 ·
  PostgreSQL). Use SEMPRE que for criar, renomear, revisar ou discutir o nome de qualquer
  artefato do projeto — classe, interface, record, enum, pacote, método, variável, entidade
  JPA, tabela, coluna, constraint, índice, migration Flyway, endpoint REST, campo JSON,
  header, variável de ambiente, bean, teste, branch, commit, arquivo Docker ou property do
  application.yml. Acione mesmo quando o usuário não falar "nomenclatura": pedidos como
  "cria o service de usuário", "adiciona uma tabela para X", "faz o endpoint de Y",
  "renomeia essa classe", "esse nome tá bom?", "revisa esse código" ou colar um trecho de
  código pedindo ajuste TODOS passam por esta skill antes de qualquer código ser escrito.
  A skill impõe o glossário canônico do domínio (realtor, customer, admin, user, role,
  address) e bloqueia sinônimos concorrentes. Nunca invente convenção nova sem registrar
  aqui.
---

# Padrão de Nomenclatura — Corretoria

Projeto: plataforma que reúne todos os corretores em um só lugar.
Raiz de pacote: `com.corretoria` · Java 21 · Spring Boot 3.5.x · PostgreSQL 16.

**Escopo:** identidade, autenticação, autorização e gestão de usuários (corretor, cliente,
administrador).

## Como usar esta skill

1. **Antes de escrever código**, identifique cada artefato novo e resolva o nome pelas
   tabelas abaixo.
2. **Valide contra o glossário canônico** — sinônimo proibido é erro, não estilo.
3. **Ao revisar código**, produza uma tabela `Atual → Proposto → Regra violada`.
4. Se a situação não estiver coberta, **derive pela regra mais próxima**, aplique, e informe
   ao usuário que a convenção precisa ser adicionada a esta skill.

---

## 1. Glossário canônico (não negociável)

O vocabulário do domínio é **inglês no código** e **português na documentação/mensagens ao
usuário**. Um conceito = um termo. Sempre.

| Conceito (PT) | Termo canônico (código) | Proibido usar |
|---------------|-------------------------|---------------|
| Corretor | `realtor` / `Realtor` / `ROLE_REALTOR` | `broker`, `agent`, `owner`, `corretor` |
| Cliente | `customer` / `Customer` / `ROLE_CUSTOMER` | `client`, `buyer`, `user` (genérico), `cliente` |
| Administrador | `admin` / `ROLE_ADMIN` | `manager`, `root`, `su`, `sysadmin` |
| Usuário (identidade genérica) | `user` / `User` | `account`, `person`, `usuario` |
| Papel de autorização | `role` / `Role` / `RoleName` | `permission`, `profile`, `perfil`, `papel` |
| Endereço | `address` / `Address` | `location`, `endereco` |
| Token de redefinição de senha | `passwordResetToken` | `recoveryToken`, `resetCode` |

> ⚠️ **O papel do corretor é `ROLE_REALTOR`.** `ROLE_OWNER` não existe no projeto; qualquer
> ocorrência dele no código é bug. `owner` também não é sinônimo aceitável de corretor.

Idioma: **nunca misture** português e inglês em um mesmo identificador
(`UserProprietario`, `salvarUser`, `usuarioRepository` → proibidos).

---

## 2. Java

### 2.1 Pacotes

`minúsculo`, sem underscore, sem plural, sem abreviação.

```
com.corretoria.<camada>[.<subcontexto>]
```

| Pacote | Conteúdo |
|--------|----------|
| `config` | `@Configuration` |
| `controller` | `@RestController` |
| `service` | regra de negócio |
| `repository` | interfaces Spring Data |
| `entity` | entidades JPA e `Auditable` |
| `enums` | enums de domínio |
| `mapper` | mapeadores MapStruct |
| `exception` | exceções de domínio + `GlobalExceptionHandler` |
| `security` | JWT e beans de autorização |
| `util` | utilitários estáticos |
| `vo.shared` | VOs de valor estáveis |
| `vo.v1.request` / `vo.v1.response` | VOs de contrato versionados |

Proibido: `dto`, `dtos`, `models`, `beans`, `helpers`, `commons`, `impl` como pacote,
`br.com.corretoria` (a raiz é `com.corretoria`).

### 2.2 Classes, interfaces, records

`PascalCase`, substantivo, **singular**, sem abreviação. Sufixo obrigatório por
responsabilidade:

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| Entidade JPA | `<Agregado>` (sem sufixo) | `User`, `Role`, `Address`, `PasswordResetToken` |
| Controller | `<Agregado>Controller` | `UserController`, `AuthController` |
| Service (classe única) | `<Agregado>Service` | `UserService`, `AuthenticationService` |
| Service com interface | interface `<Agregado>Service` + `<Agregado>ServiceImpl` | `UserServiceImpl` |
| Repository | `<Agregado>Repository` | `UserRepository`, `RoleRepository` |
| Mapper MapStruct | `<Agregado>Mapper` | `UserMapper`, `AddressMapper` |
| Assembler HATEOAS | `<Agregado>ModelAssembler` | `UserModelAssembler` |
| Config | `<Assunto>Config` | `SecurityConfig`, `OpenApiConfig` |
| Filtro | `<Assunto>Filter` | `JwtAuthenticationFilter` |
| Bean de autorização | `<Agregado>Security` | `UserSecurity` |
| Utilitário | `<Assunto>Utils` (final, construtor privado) | `TextUtils`, `ObjectUtils` |
| Exceção | `<Situação>Exception` | `DuplicateResourceException` |
| Enum | `<Conceito>` singular | `RoleName` |

Regras:
- **Interface não recebe prefixo `I`** (`IUserService` → proibido).
- Só crie `...Impl` se existir **mais de uma implementação prevista**. Interface com
  implementação única é acoplamento cerimonial.
- Sem sufixo `Manager`, `Processor`, `Handler` (exceto `GlobalExceptionHandler`), `Helper`,
  `Util` no singular, `Data`, `Info`, `Object`.

### 2.3 Value Objects

| Categoria | Pacote | Padrão | Exemplo |
|-----------|--------|--------|---------|
| VO de valor | `vo.shared` | `<Conceito>` | `Email`, `ZipCode` |
| VO de entrada | `vo.v1.request` | `<Agregado><Ação>Request` | `UserRegistrationRequest`, `UserUpdateRequest`, `PasswordChangeRequest`, `LoginRequest`, `AddressRequest` |
| VO de saída | `vo.v1.response` | `<Agregado>[Qualificador]Response` | `UserResponse`, `AddressResponse`, `RoleResponse`, `AuthResponse` |

- Ação no `Request` é **substantivo de ação**: `Registration`, `Update`, `Change` — não verbo
  (`CreateUserRequest` → proibido; use `UserRegistrationRequest`).
- **Nunca** use `DTO`, `VO`, `Payload`, `Form`, `Bean` no nome da classe.
- Todo VO é `record`. Campos em `camelCase`, sem prefixo `_`.

### 2.4 Métodos

`camelCase`, verbo no infinitivo, primeira palavra é a intenção.

| Intenção | Prefixo | Exemplo |
|----------|---------|---------|
| Consulta que pode não achar | `find` (retorna `Optional`/`Page`/`List`) | `findByLogin`, `findByName` |
| Consulta que exige existir | `get` (lança `ResourceNotFoundException`) | `getUserOrThrow` |
| Predicado | `is` / `has` / `can` / `exists` (retorna `boolean`) | `isSelf`, `existsByEmail` |
| Criação | `create` / `register` | `register` |
| Alteração | `update` / `change` / verbo de domínio | `update`, `changePassword` |
| Remoção | `delete` | `delete` |
| Validação que lança | `validate` / `ensure` | `ensureEmailIsUnique` |
| Mapeamento | `to<Destino>` / `from<Origem>` | `toResponse`, `fromRequest` |

Proibido: `get` para operação com efeito colateral, `doSomething`, `process`, `handle`,
`manage`, `execute` sem objeto, sufixo numérico (`update2`), `...ByFilter` genérico.

### 2.5 Variáveis, parâmetros, constantes

- `camelCase` descritivo. Nome de uma letra só em lambda trivial e `for` clássico.
- Coleções em **plural** (`users`, `addresses`, `roles`); item único em singular.
- `boolean` como predicado: `used`, `active`, `hasAddresses` — nunca `flag`, `status`
  booleano, `isNotDeleted` (dupla negativa).
- Constantes: `static final` em `UPPER_SNAKE_CASE`.
- Proibido: `temp`, `aux`, `obj`, `data`, `list1`, `x` (fora de escopo matemático), `str`.

### 2.6 Enums

`PascalCase` singular; constantes `UPPER_SNAKE_CASE` sem prefixo do enum.

```java
public enum RoleName { ROLE_ADMIN, ROLE_REALTOR, ROLE_CUSTOMER }
```

`RoleName` é a única exceção ao "sem prefixo": o prefixo `ROLE_` é exigido pelo Spring
Security.

---

## 3. Banco de dados (PostgreSQL)

| Artefato | Convenção | Exemplo |
|----------|-----------|---------|
| Tabela | `snake_case`, **plural** | `users`, `roles`, `addresses`, `password_reset_tokens` |
| Tabela associativa | `<tabela_a_singular>_<tabela_b_plural>` | `user_roles` |
| Coluna | `snake_case`, singular | `zip_code`, `token_hash` |
| FK (coluna) | `<entidade_singular>_id` | `user_id`, `role_id` |
| PK | sempre `id`, `BIGINT GENERATED ALWAYS AS IDENTITY` | `id` |
| Timestamp | sufixo `_at` | `created_at`, `last_updated_at`, `expires_at` |
| Autor de escrita | sufixo `_by` | `created_by`, `last_updated_by` |
| Booleano | adjetivo/particípio, sem `is_` | `used`, `active` |

Constraints e índices — nome **explícito sempre**:

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| Primary key | `pk_<tabela>` | `pk_users` |
| Foreign key | `fk_<tabela>_<tabela_referenciada>` | `fk_addresses_users` |
| Unique | `uk_<tabela>_<colunas>` | `uk_users_email`, `uk_users_login` |
| Check | `ck_<tabela>_<regra>` | `ck_addresses_state_length` |
| Índice | `idx_<tabela>_<colunas>` | `idx_users_name` |

Proibido: `tb_`/`tbl_` como prefixo de tabela, coluna com nome de tipo (`varchar_field`),
plural em coluna, acento, camelCase, abreviação (`desc`, `qtd`, `dt`).

### Migrations Flyway

```
V<n>__<verbo>_<objeto>.sql          # dois underscores após a versão
```

| Verbo | Uso | Exemplo |
|-------|-----|---------|
| `create` | nova tabela | `V1__create_initial_schema.sql` |
| `add` | nova coluna/constraint/índice | `V5__add_audit_columns.sql` |
| `alter` | mudança de coluna existente | `V6__alter_users_login_length.sql` |
| `drop` | remoção | `V7__drop_users_legacy_column.sql` |
| `seed` | dados | `V2__seed_demo_users.sql` |
| `rename` | renomeação | `V8__rename_users_status_column.sql` |

Regras: versão inteira sequencial, sem lacuna, sem reaproveitar número; descrição em
`snake_case` inglês; **migration aplicada é imutável** (mudança = nova migration).

---

## 4. API REST

| Artefato | Convenção | Exemplo |
|----------|-----------|---------|
| Base | `/api/v{n}` | `/api/v1` |
| Coleção | substantivo **plural**, `kebab-case` | `/api/v1/users` |
| Recurso | `/{id}` (nunca `/get/{id}`) | `/api/v1/users/42` |
| Subrecurso | plural aninhado | `/api/v1/users/42/addresses` |
| Ação sobre atributo | `PATCH` + substantivo do atributo | `/api/v1/users/42/password` |
| Query param | `camelCase` | `?name=joao` |
| Paginação | `page`, `size`, `sort` (padrão Spring) | `?page=0&size=20&sort=name,asc` |

Regras:
- **Sem verbo em substantivo de rota** (`/createUser`, `/user/get` → proibidos).
- Sem `/api/v1/user` (singular) nem `/Users` (maiúscula).
- Sem barra final.
- Campos JSON em `camelCase`, espelhando o `record` do VO. Nunca `snake_case` no contrato.
- Headers customizados: `X-Corretoria-<Assunto>` em `Train-Case`.

---

## 5. Configuração e infraestrutura

| Artefato | Convenção | Exemplo |
|----------|-----------|---------|
| Property (`application.yml`) | `kebab-case` sob namespace `corretoria.` | `corretoria.jwt.expiration` |
| Variável de ambiente | `UPPER_SNAKE_CASE` | `JWT_SECRET`, `DB_PASSWORD` |
| Profile Spring | minúsculo | `dev`, `test`, `prod` |
| Serviço no compose | minúsculo curto | `app`, `db` |
| Imagem Docker | `corretoria-<módulo>:<tag>` | `corretoria-api:1.0.0` |
| Banco | `corretoria-app` | — |

Nunca coloque segredo literal em `application.yml` — sempre `${VAR:default-de-exemplo}`.

---

## 6. Testes

| Artefato | Convenção | Exemplo |
|----------|-----------|---------|
| Classe de teste unitário | `<ClasseSobTeste>Test` | `UserServiceTest`, `UserSecurityTest` |
| Teste de integração | `<Assunto>IT` ou `<Assunto>IntegrationTest` | `UserControllerIT` |
| Teste de arquitetura | `ArchitectureTest` | — |
| Método de teste | `camelCase` descrevendo cenário + resultado | `shouldRejectRegistrationWhenRoleAdminIsRequested` |
| `@DisplayName` | português, `<método> - <cenário> - <esperado>` | `"register - papel ADMIN solicitado - lança ForbiddenOperationException"` |
| Bloco `@Nested` | `PascalCase` = nome do método sob teste | `class Register { ... }` |
| Fixture/builder | `a<Entidade>` / `<Entidade>Fixture` | `aRealtorUser()` |

Estrutura interna: comentários `// arrange`, `// act`, `// assert` (ou Given/When/Then),
sempre nessa ordem.

---

## 7. Git

| Artefato | Convenção | Exemplo |
|----------|-----------|---------|
| Branch | `<tipo>/<escopo>-<descrição-curta-kebab>` | `feat/user-password-change` |
| Tipos de branch | `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf` | — |
| Commit | Conventional Commits, imperativo, PT ou EN consistente | `feat(user): adiciona troca de senha` |
| Escopo do commit | agregado ou camada | `user`, `auth`, `security`, `db` |
| Tag | `v<major>.<minor>.<patch>` | `v1.0.0` |

---

## 8. Antipadrões — rejeitar em revisão

| Antipadrão | Correto |
|------------|---------|
| `ROLE_OWNER` para corretor | `ROLE_REALTOR` |
| `BrokerService`, `UsuarioService` | `UserService` |
| `UserDTO`, `UserPayload` | `UserResponse` / `UserRegistrationRequest` |
| `IUserService` | `UserService` |
| `CreateUserRequest` | `UserRegistrationRequest` |
| `tb_users`, `User` (tabela) | `users` |
| `dataCriacao`, `dtCriacao` | `created_at` / `createdAt` |
| `POST /api/v1/createUser` | `POST /api/v1/users` |
| `GET /api/v1/user/{id}` | `GET /api/v1/users/{id}` |
| `{ "zip_code": "60000000" }` | `{ "zipCode": "60000000" }` |
| `V5_add_audit_columns.sql` (um underscore) | `V5__add_audit_columns.sql` |
| `getUsers()` que salva algo | separar consulta de comando |
| `isNotInactive` | `active` |
| `utils/Helper.java` | `util/TextUtils.java` |

---

## 9. Formato de saída ao revisar nomes

```
### Ajustes de nomenclatura

| Atual | Proposto | Regra |
|-------|----------|-------|
| `UsuarioService` | `UserService` | §1.1 glossário — `user` é canônico; sem português no identificador |
| `tb_usuario` | `users` | §3 — tabela em snake_case plural, sem prefixo `tb_` |

### Conforme o padrão
- `UserMapper`, `uk_users_email`

### Convenção ausente nesta skill
- (se houver) descrever o caso e a regra derivada aplicada
```

Se **nada** violar o padrão, diga apenas isso — não invente ajuste cosmético.
