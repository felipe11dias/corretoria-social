# Requisitos — Corretoria Fase 1: Plataforma de Identidade

**Autor:** Felipe Dias Mac Dowell
**Data:** 2026-07-31
**Status:** retrospectivo — descreve requisitos já implementados e verificados. Para a narrativa de execução etapa a etapa, defeitos encontrados e evidências, ver [`docs/relatorios/relatorio-corretoria-v1_0.md`](../relatorios/relatorio-corretoria-v1_0.md). Para a próxima fase, ver [`docs/requisitos/requisitos-corretoria-v2_0.md`](requisitos-corretoria-v2_0.md).

## 1. Contexto e objetivo

A Fase 1 entregou uma **plataforma de identidade**: autocadastro, autenticação JWT,
autorização por posse, gestão de usuários por administrador, auditoria de escrita,
versionamento de schema e uma API REST versionada e documentada. Concluída em 26/07/2026, em
15 etapas, com 136 testes e 32 regras de ArchUnit.

Este documento reformula os requisitos que essa fase cumpriu no mesmo formato usado para a
Fase 2 (papéis, glossário, modelo de dados, regras de negócio, API, decisões) — não para
substituir o relatório técnico, mas para que as duas fases formem uma série coerente e
comparável. Nada aqui é novo: é uma leitura em formato de requisitos do que já foi construído
e verificado.

## 2. Papéis e matriz de permissões

| Ação | Anônimo | `ROLE_CUSTOMER` | `ROLE_REALTOR` | `ROLE_ADMIN` |
|---|---|---|---|---|
| Login (`POST /auth/login`) | ✅ | — | — | — |
| Autocadastro (`POST /users`) | ✅ — vira `ROLE_CUSTOMER` ou `ROLE_REALTOR` conforme solicitado | — | — | ❌ 403 — autocadastro nunca concede `ROLE_ADMIN` |
| Consultar usuário por id | ❌ | ✅ (só o próprio) | ✅ (só o próprio) | ✅ (qualquer) |
| Listar/buscar usuários por nome | ❌ | ❌ | ❌ | ✅ |
| Atualizar nome/endereços do próprio cadastro | ❌ | ✅ | ✅ | ✅ (qualquer) |
| Trocar a própria senha (exige senha atual) | ❌ | ✅ | ✅ | ✅ (qualquer) |
| Excluir o próprio cadastro | ❌ | ✅ | ✅ | ✅ (qualquer) |

`ROLE_ADMIN` só nasce por seed de banco (migration `V3`) ou por ação de outro admin — nunca
por autocadastro público. Um usuário pode acumular mais de um papel simultaneamente.

## 3. Glossário de nomenclatura (fundação)

Fonte de verdade: `.claude/skills/corretoria-naming-standards/SKILL.md`. Reproduzido aqui por
completude — a extensão para a Fase 2 (`listing`, `property`, `vehicle` etc.) está na seção 4
de `requisitos-corretoria-v2_0.md`.

| Conceito (PT) | Termo canônico | Proibido |
|---|---|---|
| Corretor | `realtor` / `Realtor` / `ROLE_REALTOR` | `broker`, `agent`, `owner`, `corretor` |
| Cliente | `customer` / `Customer` / `ROLE_CUSTOMER` | `client`, `buyer`, `user` genérico, `cliente` |
| Administrador | `admin` / `ROLE_ADMIN` | `manager`, `root`, `su`, `sysadmin` |
| Usuário (identidade genérica) | `user` / `User` | `account`, `person`, `usuario` |
| Papel de autorização | `role` / `Role` / `RoleName` | `permission`, `profile`, `perfil`, `papel` |
| Endereço | `address` / `Address` | `location`, `endereco` |
| Token de redefinição de senha | `passwordResetToken` | `recoveryToken`, `resetCode` |

> `ROLE_OWNER` não existe no projeto — é bug se aparecer. O papel do corretor é sempre
> `ROLE_REALTOR`.

## 4. Modelo de dados

Convenções: `id` `Long` `IDENTITY`; toda entidade estende `Auditable`
(`createdAt`/`lastUpdatedAt`/`createdBy`/`lastUpdatedBy`, preenchidos automaticamente) exceto
`user_roles`, que é junção pura sem auditoria; constraints nomeadas explicitamente
(`pk_`/`fk_`/`uk_`/`ck_`/`idx_` + tabela + colunas); schema criado só pelo Flyway
(`ddl-auto: validate`).

**`User`** (`users`) — único agregado raiz de identidade; corretor, cliente e administrador
são todos `User`, o que distingue é a associação a `Role`.

| Campo | Tipo | Regra |
|---|---|---|
| name | `String` | NOT NULL |
| email | `String` | NOT NULL, único, sempre minúsculo (normalizado pelo VO `Email`) |
| login | `String` | NOT NULL, único, **imutável após o cadastro** |
| password | `String` | NOT NULL — hash BCrypt, nunca texto plano |
| roles | `Set<Role>` | `@ManyToMany` via `user_roles`, `fetch=EAGER` |
| addresses | `List<Address>` | `@OneToMany`, cascade `ALL` + `orphanRemoval`, `fetch=LAZY` |
| passwordResetTokens | `List<PasswordResetToken>` | `@OneToMany`, cascade `ALL` + `orphanRemoval` |

**`Role`** (`roles`) — entidade própria, não enum de coluna, para acumular papéis sem mudar
schema e porque é a estrutura que o Spring Security consome.

| Campo | Tipo | Regra |
|---|---|---|
| name | `RoleName` (`ROLE_ADMIN`, `ROLE_REALTOR`, `ROLE_CUSTOMER`) | NOT NULL, único, `@Enumerated(STRING)` |

**`Address`** (`addresses`) — dependente de `User`, sem repository próprio (nasce, muda e
morre junto do dono, por cascade e `orphanRemoval`).

| Campo | Tipo | Regra |
|---|---|---|
| user | `User` (FK) | NOT NULL |
| street, city, state | `String` | NOT NULL (`state` com 2 caracteres, CHECK) |
| number, complement, neighborhood | `String` | opcionais |
| zipCode | `String` | NOT NULL, 8 dígitos sem máscara (CHECK regex), formatação só na exibição |

**`PasswordResetToken`** (`password_reset_tokens`) — dependente de `User`, com repository
próprio.

| Campo | Tipo | Regra |
|---|---|---|
| user | `User` (FK) | NOT NULL |
| tokenHash | `String` | NOT NULL, único — guarda o hash, nunca o token enviado ao usuário |
| expiresAt | `LocalDateTime` | NOT NULL |
| used | `boolean` | NOT NULL |

## 5. Regras de negócio consolidadas

1. E-mail e login são definidos no cadastro e **imutáveis** depois — só nome e endereços são
   editáveis via `PUT /users/{id}` (decisão tomada durante a Etapa 10, aplicada
   retroativamente ao contrato).
2. Autocadastro público nunca concede `ROLE_ADMIN` — rejeitado com `403`.
3. Senha sempre armazenada como hash BCrypt; troca de senha exige confirmação da senha atual.
4. Identidade/posse sempre resolvida pelo login do usuário autenticado no token
   (`UserSecurity.isSelf`), nunca por um id vindo do payload ou da URL — proteção explícita
   contra IDOR.
5. Um usuário pode acumular múltiplos papéis (ex.: `ROLE_REALTOR` e `ROLE_CUSTOMER` ao mesmo
   tempo).
6. Papéis não vão dentro do JWT — são recarregados do banco a cada requisição
   (`CustomUserDetailsService`), então uma revogação de papel vale imediatamente, sem esperar o
   token expirar.
7. Exclusão de um usuário faz cascade sobre `addresses` e `passwordResetTokens`.
8. Gestão de usuários (listar, buscar, atualizar ou excluir qualquer um) é exclusiva de
   `ROLE_ADMIN`; os demais papéis só gerenciam o próprio cadastro.

## 6. Esboço da API REST (implementada)

| Recurso | Método + rota | Auth | Retornos |
|---|---|---|---|
| Auth | `POST /api/v1/auth/login` | Pública | 200/400/401 |
| Usuários | `POST /api/v1/users` | Pública (recusa `ROLE_ADMIN`) | 201/400/403/409 |
| | `GET /api/v1/users/{id}` | Dono ou `ROLE_ADMIN` | 200/401/403/404 |
| | `GET /api/v1/users` (+`?name=`) | `ROLE_ADMIN` | 200/401/403 |
| | `PUT /api/v1/users/{id}` (nome + endereços) | Dono ou `ROLE_ADMIN` | 200/400/401/403/404 |
| | `PATCH /api/v1/users/{id}/password` | Dono ou `ROLE_ADMIN` | 204/400/401/403/404 |
| | `DELETE /api/v1/users/{id}` | Dono ou `ROLE_ADMIN` | 204/401/403/404 |

Paginação padrão do Spring (`page`/`size`/`sort`, default `sort=name`); respostas
envelopadas em HATEOAS (`EntityModel`/`PagedModel`); contrato OpenAPI gerado em tempo de
execução pelo springdoc a partir dos controllers e do Bean Validation dos VOs
(`/v3/api-docs`, `/swagger-ui.html`); coleção Postman com 23 requisições cobrindo casos felizes
e de erro.

## 7. Considerações técnicas e de arquitetura

- **Stack:** Java 21, Spring Boot 3.5.16, PostgreSQL 16, Flyway, MapStruct 1.6.3,
  springdoc-openapi 2.8.17, JWT (jjwt 0.12.6), Spring Security, HATEOAS, Lombok, JUnit 5 +
  Mockito + AssertJ + ArchUnit 1.4.1.
- **Arquitetura em camadas**, 32 regras de ArchUnit com poder de veto:
  `controller → service → repository → entity/enums`, transversais `vo` (records), `mapper`
  (MapStruct), `exception` (`ProblemDetail`/RFC 7807), `security`, `config`, `util`.
- **Sem DTO** — proibido pelo nome. Só VO `record`: `vo.shared` para VO de valor (`Email`,
  `ZipCode`, com validação/normalização no construtor compacto), `vo.v1.request`/
  `vo.v1.response` para contrato de API.
- **Autenticação:** JWT HS256 stateless, token carrega só `subject` (login) + `issuedAt` +
  `expiration` — nenhum papel no token.
- **Erros:** `GlobalExceptionHandler` (`@RestControllerAdvice`) unificando toda resposta de
  erro em `ProblemDetail`, com `ProblemType` catalogando 10 tipos (`VALIDATION_ERROR`,
  `RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`, `FORBIDDEN_OPERATION` etc.).
- **Build:** `maven-surefire-plugin` fixado em 3.5.2 (a versão gerenciada pelo Spring Boot
  3.5.3 descarta silenciosamente os testes de campo do ArchUnit).

## 8. Decisões e justificativas

Condensação das decisões técnicas registradas no relatório original (seção "Decisões
Técnicas", `docs/relatorios/relatorio-corretoria-v1_0.md`):

| Decisão | Justificativa |
|---|---|
| Papel do corretor é sempre `ROLE_REALTOR` | Glossário fechado desde a Etapa 1 — sinônimos (`broker`, `agent`, `owner`) proibidos para evitar que o mesmo conceito ganhe dois nomes |
| Versionamento de API por path (`/api/v1/...`) | Padrão explícito e visível na própria rota |
| `ProblemDetail` (RFC 7807) para todo erro | Nativo do Spring 6+, evita reinventar um formato de erro |
| JWT stateless, senha BCrypt, HMAC-SHA256 | Sem sessão/cookie — elimina CSRF por construção |
| Autorização por posse (`UserSecurity`) + autocadastro restrito | Evita IDOR; autocadastro público nunca concede `ROLE_ADMIN` |
| Papéis em tabela associativa, não enum de coluna | Acúmulo de papéis sem mudança de schema; é a estrutura que o Spring Security consome |
| Schema versionado por Flyway, Hibernate em `validate` | Uma única fonte de verdade para o schema, sem geração automática de DDL |
| HATEOAS sobre VOs (`EntityModel`/`CollectionModel`) | Enriquece a resposta REST preservando a imutabilidade dos records |
| MapStruct para mapeamento Entity↔VO | Geração em tempo de compilação, suporte nativo a records, zero reflection — DozerMapper descartado por incompatível com records imutáveis |
| Todo objeto de fronteira é VO (`record`) | `vo/v1` versionado para contrato, `vo/shared` estável para VO de valor |
| Evidência por par (endpoint, retorno), não só por endpoint | Um `200` no caminho feliz pode esconder autorização ou contrato de erro quebrados |
| Defeito de revisão corrigido na etapa de origem | Sempre acompanhado de um teste que falharia antes da correção |

## 9. Pendências conhecidas

`PasswordResetToken` tem entidade, tabela e repository migrados desde a Etapa 4/5, mas
**nenhum service ou controller usa esse fluxo ainda** — "esqueci minha senha" não é uma
funcionalidade utilizável hoje, só infraestrutura preparada para receber uma futura
implementação.

## Referências

- [`docs/relatorios/relatorio-corretoria-v1_0.md`](../relatorios/relatorio-corretoria-v1_0.md) — relatório técnico completo, narrativa das 15 etapas, defeitos e evidências.
- [`docs/requisitos/requisitos-corretoria-v2_0.md`](requisitos-corretoria-v2_0.md) — requisitos da Fase 2 (domínio de anúncios de imóveis e automóveis).
- `.claude/skills/corretoria-naming-standards/SKILL.md`, `corretoria-architecture-guard/SKILL.md`, `corretoria-java-clean-solid/SKILL.md` — as três skills de governança.
