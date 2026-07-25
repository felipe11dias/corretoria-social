---
name: corretoria-architecture-guard
description: >
  Guardiã da integridade arquitetural do projeto Corretoria (Arquitetura em Camadas ·
  Java 21 · Spring Boot 3.5 · PostgreSQL) e das regras automatizadas de ArchUnit. Use
  SEMPRE que uma mudança puder afetar a estrutura do projeto: criar classe, interface,
  record ou pacote novo; decidir em qual camada algo mora; injetar uma dependência em outra
  classe; escrever, corrigir ou revisar testes de ArchUnit; investigar falha de
  `ArchitectureTest`; discutir se algo "pode chamar" outra coisa; propor um novo pacote,
  módulo ou padrão. Acione também com pedidos indiretos como "cria o service de X",
  "onde eu coloco essa classe", "o controller precisa buscar isso no banco",
  "revisa a arquitetura", "adiciona esse método na entidade" ou ao colar código para
  revisão. Esta skill tem poder de veto: se a mudança violar a regra de dependência, ela é
  recusada e uma alternativa conforme é proposta ANTES de qualquer código ser escrito.
---

# Guarda de Arquitetura — Corretoria

Arquitetura: **Layered Architecture** (camadas), raiz `com.corretoria`.
Enforcement: **ArchUnit 1.4.x** em `ArchitectureTest`, executado em `mvn test`.

**Escopo:** identidade, autenticação, autorização e gestão de usuários. Toda evolução deve
caber nesta mesma estrutura de camadas, sem exceção nova.

## Como usar esta skill

1. Classifique cada artefato novo/alterado em uma camada (§1).
2. Confira a **regra de dependência** (§2). Violação = veto + alternativa conforme.
3. Verifique as **invariantes por camada** (§3).
4. Se a mudança introduzir um padrão novo (novo pacote, nova categoria de classe),
   **escreva a regra de ArchUnit correspondente** (§5) na mesma entrega. Regra sem teste é
   convenção que apodrece.
5. Ao final, emita o **parecer arquitetural** (§7).

---

## 1. Camadas e o que mora em cada uma

| # | Camada | Pacote | Contém | Nunca contém |
|---|--------|--------|--------|--------------|
| 1 | Apresentação | `controller` | `@RestController`, validação de entrada via VO, montagem de resposta HATEOAS, `@PreAuthorize` | regra de negócio, SQL, acesso a `Repository`, entidade JPA no contrato |
| 2 | Negócio | `service` | regra de negócio, orquestração, transações (`@Transactional`), tradução VO ↔ entidade via mapper | anotação web (`@RequestMapping`, `HttpServletRequest`), `ResponseEntity`, montagem de link HATEOAS |
| 3 | Persistência | `repository` | interfaces `JpaRepository`, query methods, `@Query` | regra de negócio, dependência de VO ou controller |
| 4 | Domínio | `entity`, `enums` | entidades JPA, `Auditable`, enums de domínio, invariantes locais da entidade | dependência de service, controller, repository, VO ou Spring Web |

### Transversais (acessíveis de mais de uma camada, com restrições)

| Componente | Pacote | Quem pode depender |
|------------|--------|--------------------|
| VOs de contrato | `vo.v1.request`, `vo.v1.response` | `controller`, `service`, `mapper` |
| VOs de valor | `vo.shared` | qualquer camada exceto `entity` (a entidade guarda tipos primitivos/JPA) |
| Mapeadores | `mapper` | `service` |
| Exceções | `exception` | todas |
| Segurança | `security` | `config`, `controller` (via SpEL em `@PreAuthorize`), `service` |
| Configuração | `config` | ninguém depende de `config`; `config` depende de quem precisar |
| Utilitários | `util` | todas; `util` não depende de nada do projeto |

---

## 2. Regra de dependência (o coração)

```
controller ──► service ──► repository ──► entity
     │            │                          ▲
     │            └──► mapper ──► vo ────────┘  (mapper conhece ambos)
     └──► vo
```

**Permitido**

- `controller` → `service`, `vo`, `exception`, `security`, `util`
- `service` → `repository`, `entity`, `enums`, `mapper`, `vo`, `exception`, `util`, `security`
- `repository` → `entity`, `enums`
- `mapper` → `entity`, `vo`, `enums`
- `entity` → `entity`, `enums`, `jakarta.persistence`, `java.*`
- `util` → apenas `java.*`
- `security` → `repository`, `entity`, `enums`, `exception`, `config`, `util`

**Por que `security` lê o repository direto.** `CustomUserDetailsService` e
`UserSecurity.isSelf` precisam resolver o usuário autenticado a cada requisição. Passar pelo
`service` criaria ciclo (`UserService` depende do `PasswordEncoder`, declarado no
`SecurityConfig`) sem ganho: não há regra de negócio nessa leitura, é resolução de
identidade. A segurança é transversal e fica **abaixo** da apresentação — por isso
`security` → `controller` continua proibido, e há regra de ArchUnit
(`securityNaoDependeDeController`) fixando isso.

**Proibido (veto imediato)**

| Violação | Por quê |
|----------|---------|
| `controller` → `repository` | pula a camada de negócio; regra nasce no controller e se duplica |
| `service` → `controller` | inversão de fluxo; acopla negócio ao transporte |
| `repository` → `service` / `vo` / `controller` | persistência não conhece camadas superiores |
| `entity` → `service` / `controller` / `repository` / `vo` | domínio no centro; nada aponta para fora |
| `util` → qualquer pacote do projeto | utilitário deve ser folha, sem ciclo |
| ciclo entre pacotes | erosão arquitetural garantida |
| entidade JPA em assinatura de método `public` de `controller` | vaza schema e senha para a API |
| `jakarta.servlet` / `org.springframework.web` em `service` ou `entity` | contamina negócio/domínio com transporte |

**Se o usuário pedir uma violação**, não implemente. Responda com:
o que foi pedido → qual regra viola → **a alternativa conforme** (normalmente: mover a
lógica para o service, criar um método de consulta no service, ou extrair um VO).

---

## 3. Invariantes por camada

### Controller
- Anotado com `@RestController` e `@RequestMapping("/api/v1/...")`.
- Recebe e devolve **somente VO** (ou `EntityModel`/`PagedModel` sobre VO).
- Sem `try/catch` de exceção de domínio — o `GlobalExceptionHandler` centraliza.
- Sem `@Transactional`.
- Autorização declarativa por `@PreAuthorize`; nunca `if (user.getRole()...)` no corpo.

### Service
- Anotado com `@Service`; escrita em `@Transactional`, leitura em
  `@Transactional(readOnly = true)`.
- Injeção **por construtor** (campos `final`). Nunca `@Autowired` em campo.
- Nunca devolve entidade para fora; converte via mapper.
- Resolve o usuário autenticado pelo contexto de segurança — **nunca confia em id de dono
  vindo do corpo da requisição**. Regra crítica do Corretoria: identidade e posse vêm do
  token, não do payload.

### Repository
- `interface` estendendo `JpaRepository<Entidade, Long>`. Nunca classe concreta.
- Sem lógica: apenas query methods derivados, `@Query` ou `Specification`.
- Um repository por **agregado raiz** (`User`, `Role`, `PasswordResetToken`). Entidades
  filhas (`Address`) são gerenciadas pelo agregado via cascade / `orphanRemoval`.

### Entity
- `@Entity` + `@Table(name = "...")` explícito; estende `Auditable`.
- Sem `@JsonProperty`, `@Schema`, Bean Validation de contrato ou qualquer anotação de API.
- Sem `@Autowired`, sem chamada a bean.
- Coleções inicializadas; `equals`/`hashCode` por `id` (nunca por coleção).
- Regras que dependem de outra entidade ou de consulta ficam no service.

### VO
- Todo VO é `record` (imutável). Sem setter, sem construtor vazio.
- `Request` valida com Bean Validation; `Response` nunca expõe senha, hash ou token
  (exceto `AuthResponse`, que carrega o JWT emitido).
- VO não conhece entidade e não tem lógica de persistência.

---

## 4. Decisões arquiteturais registradas

| Decisão | Motivo |
|---------|--------|
| Camadas, não Hexagonal/Clean | escopo com regras diretas; menor complexidade acidental com a mesma separação exigida pelo SOLID |
| VO em vez de DTO | imutabilidade e igualdade por valor do `record` Java 21 |
| MapStruct em vez de mapeamento manual/reflexivo | geração em compilação, suporte a `record`, zero reflection |
| Flyway com `ddl-auto: validate` | schema é artefato versionado, não efeito colateral do Hibernate |
| Autorização por posse + escopo administrativo | evita *IDOR*; gestão de usuários é do admin |
| Papéis em tabela associativa | permite acúmulo de papéis sem mudança de schema |
| `ProblemDetail` (RFC 7807) | contrato de erro único e padronizado |

Ao propor uma mudança que contrarie qualquer linha acima, **explicite o trade-off e peça
confirmação** antes de implementar.

---

## 5. Regras de ArchUnit — `ArchitectureTest`

Local: `src/test/java/com/corretoria/architecture/ArchitectureTest.java`.
Referência completa e compilável: `reference/ArchitectureTest.java` (nesta skill).

Toda regra abaixo deve existir. Ao adicionar um pacote/categoria nova, adicione a regra
correspondente na mesma entrega.

| Grupo | Regra | Método |
|-------|-------|--------|
| Camadas | Acesso respeita `controller → service → repository → entity` | `layeredArchitecture()` |
| Camadas | Controller não acessa repository | `noClasses().that().resideInAPackage("..controller..")` |
| Camadas | Service não depende de controller | idem |
| Domínio | Entity não depende de service/controller/repository/vo | idem |
| Domínio | Entity sem anotação de API (`@Schema`, `@JsonProperty`) | `noClasses().should().dependOnClassesThat()...` |
| Contrato | VO não depende de entity | `noClasses()...` |
| Contrato | Todo VO é `record` | `classes().should().beRecords()` |
| Contrato | Controller não retorna entity | `noMethods()...haveRawReturnType(...)` |
| Contrato | `Response` não expõe senha/hash (exceto `AuthResponse`) | `noFields()...` |
| Persistência | Repository é interface e estende `JpaRepository` | `classes().should().beInterfaces()` / `beAssignableTo` |
| Transporte | Service/entity/repository sem `org.springframework.web` e `jakarta.servlet` | `noClasses()...` |
| Transação | `@Transactional` só em `service` | `classes().that().areAnnotatedWith(Transactional.class).should().resideInAPackage("..service..")` |
| Injeção | Sem `@Autowired` em campo; campos de dependência `final` | `noFields().should().beAnnotatedWith(Autowired.class)` |
| Utilitário | `util` não depende de pacote do projeto | `noClasses()...` |
| Ciclos | Nenhum ciclo entre pacotes | `slices().matching("com.corretoria.(*)..").should().beFreeOfCycles()` |
| Nomenclatura | Sufixo bate com o pacote (`*Controller` em `..controller..` etc.) | `classes().that().haveSimpleNameEndingWith(...)` |
| Nomenclatura | Sem interface prefixada com `I`; sem `DTO` no nome | `noClasses()...` |
| Segurança | Resolução do usuário autenticado só em `security`/`service` | `noClasses()...dependOnClassesThat().haveFullyQualifiedName("...SecurityContextHolder")` |

Diretrizes:
- Regra deve ter **mensagem em português** via `.as(...)` — a falha tem que ensinar, não só
  apontar.
- Use `.allowEmptyShould(true)` só onde o pacote pode legitimamente estar vazio hoje.
- `freeze()` (`FreezingArchRule`) só é aceitável para dívida herdada, com justificativa
  escrita no teste e prazo. Nunca para violação nova.
- Nunca "conserte" um teste de arquitetura relaxando a regra. Conserte o código. Se a regra
  estiver errada, altere-a explicitamente, com justificativa registrada em §4.

### ⚠️ Verifique a contagem de testes, não só o "BUILD SUCCESS"

O modo de falha mais perigoso não é a regra que quebra — é a regra que **não roda**. O
Surefire 3.5.3 (versão gerenciada pelo Spring Boot 3.5.3) descarta silenciosamente os
descritores de teste baseados em campo da engine do ArchUnit: a suíte reporta
`Tests run: 0`, o build fica **verde** e nenhuma regra de arquitetura é checada. Por isso o
`pom.xml` fixa `maven-surefire-plugin` em **3.5.2**.

Regra prática: ao rodar `mvn test`, confirme que o `ArchitectureTest` reporta **o número de
regras que existem na classe**. `Tests run: 0` com build verde é falha, não sucesso. Refaça
essa conferência sempre que o Spring Boot, o Surefire ou o ArchUnit forem atualizados.

Enquanto o projeto é construído etapa a etapa, dois relaxamentos temporários estão ativos e
devem ser **removidos na Etapa 12**, quando todos os pacotes estiverem populados:

| Relaxamento | Onde | Por quê |
|-------------|------|---------|
| `archRule.failOnEmptyShould=false` | `src/test/resources/archunit.properties` | pacote ainda vazio faria a regra falhar por ausência de código, não por violação |
| `.withOptionalLayers(true)` | regra `layeredArchitecture()` | camada ainda vazia seria reportada como violação |

### Código gerado também é código

As regras valem para o que o build gera, não só para o que se escreve à mão — e é assim que
deve ser: `UserMapperImpl` é um bean Spring como qualquer outro.

Caso concreto: o MapStruct, por padrão, injeta mapeadores auxiliares com `@Autowired` **em
campo**, o que viola `semAutowiredEmCampo`. A correção é na origem — no `pom.xml`:

```xml
<arg>-Amapstruct.defaultInjectionStrategy=constructor</arg>
```

Com isso o gerado passa a usar construtor e campo `final`, conforme o projeto. **Nunca
exclua o pacote `mapper` das regras para silenciar esse erro:** o problema é a configuração
do gerador, não a regra.

---

## 6. Quando `ArchitectureTest` falha

1. Leia a violação: classe de origem, classe de destino, tipo de acesso.
2. Classifique:
   - **Código errado** (caso comum) → mova a lógica para a camada correta.
   - **Regra desatualizada** → só se a arquitetura mudou por decisão consciente; atualize §4
     e a regra juntas.
3. Correções típicas:

| Violação | Correção |
|----------|----------|
| Controller chama repository | criar método no service e delegar |
| Service devolve entidade | mapear para `Response` no service |
| Entity importa VO | mover a conversão para o mapper |
| Ciclo `service ↔ service` | extrair a regra compartilhada para um service de menor nível ou uma policy sem estado |
| `@Transactional` em controller | mover a fronteira transacional para o service |
| `SecurityContextHolder` no controller | expor a identidade autenticada via parâmetro/`UserSecurity` e resolver no service |

---

## 7. Formato do parecer arquitetural

```
### Parecer arquitetural

**Classificação dos artefatos**
| Artefato | Camada | Pacote |
|---|---|---|
| `UserService` | Negócio | `com.corretoria.service` |

**Regra de dependência:** ✅ conforme | ⛔ violada
(se violada) Pedido: <o que foi pedido> · Regra: <§2, linha> · Alternativa conforme: <...>

**Invariantes:** lista do que foi verificado (construtor, @Transactional, VO, etc.)

**Cobertura ArchUnit:** regra existente reaproveitada, ou nova regra a adicionar
(com o snippet)

**Risco de erosão:** baixo | médio | alto — e o que observar
```
