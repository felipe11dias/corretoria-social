---
name: corretoria-java-clean-solid
description: >
  Padrão obrigatório de escrita de código Java do projeto Corretoria: recursos idiomáticos
  do **Java 21 (LTS)**, **Clean Code** e os **princípios SOLID**. Use SEMPRE que qualquer
  código Java for escrito, alterado, refatorado ou revisado no projeto — sem exceção. Isso
  inclui: criar ou editar service, controller, repository, entidade, VO/record, enum,
  mapper, exceção, classe de configuração ou teste; implementar uma nova feature ou regra
  de negócio; corrigir bug; extrair método ou classe; revisar PR; responder "esse código
  está bom?", "como melhorar isso?", "refatora isso", "otimiza", "aplica boas práticas".
  Acione também quando o usuário simplesmente colar uma classe Java, descrever uma regra de
  negócio a implementar, ou pedir "cria o service de X" — a skill define COMO o código deve
  ser escrito antes de escrevê-lo, e é aplicada a cada nova implementação para manter a
  integridade da linguagem, do SOLID e do clean code ao longo do tempo.
---

# Java 21 · Clean Code · SOLID — Corretoria

Java 21 (LTS) · Spring Boot 3.5.x · Maven · JUnit 5 · Mockito · AssertJ.

**Regra zero:** esta skill se aplica a *toda* linha de Java do projeto. Antes de escrever,
percorra o checklist §6. Ao revisar, entregue o parecer §7.

Skills irmãs: `corretoria-naming-standards` (nomes) e `corretoria-architecture-guard`
(camadas). Conflito entre elas não deve existir — se aparecer, pare e sinalize.

---

## 1. Java 21 — o que usar por padrão

### 1.1 `record` para dados imutáveis

Todo VO é `record`. Valide no construtor compacto:

```java
public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");

    public Email {
        Objects.requireNonNull(value, "O e-mail é obrigatório");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("E-mail inválido: " + value);
        }
    }
}
```

A normalização dentro do construtor é o que torna a regra de e-mail único **correta** — não
é detalhe cosmético. Nunca crie classe mutável com getter/setter para transporte de dados.

### 1.2 `sealed` + pattern matching para hierarquias fechadas

Use quando o conjunto de variantes é conhecido e fechado. O `switch` exaustivo elimina o
`else` defensivo e falha em compilação ao surgir uma variante nova.

```java
public sealed interface RegistrationResult permits Created, Rejected {
    record Created(Long userId) implements RegistrationResult {}
    record Rejected(String reason) implements RegistrationResult {}
}

String describe(RegistrationResult result) {
    return switch (result) {
        case RegistrationResult.Created c  -> "Criado: " + c.userId();
        case RegistrationResult.Rejected r -> "Recusado: " + r.reason();
    };
}
```

### 1.3 `switch` de expressão com pattern matching

```java
// Java 21 — expressão, exaustivo, sem fall-through
String describe(RoleName role) {
    return switch (role) {
        case ROLE_REALTOR  -> "Corretor";
        case ROLE_CUSTOMER -> "Cliente";
        case ROLE_ADMIN    -> "Administrador";
    };
}
```

Proibido: `switch` com `break`, `default` que engole caso novo silenciosamente, cadeia de
`if/else if` sobre enum.

### 1.4 `var` — com critério

Use quando o tipo é óbvio pelo lado direito. Não use quando esconde a intenção.

```java
var user = userRepository.findById(id).orElseThrow(...);   // ✅ óbvio
var result = service.process(input);                        // ⛔ opaco
```

### 1.5 `Optional` — só como retorno

```java
// ✅
public User getUserOrThrow(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
}

// ⛔ proibido
if (optional.isPresent()) { ... }         // use map/orElseThrow/ifPresent
optional.get();                            // sem isPresent = bomba
void save(Optional<User> user)             // Optional em parâmetro
private Optional<String> name;             // Optional em campo
```

### 1.6 Text blocks para SQL/JSON literal

```java
@Query(value = """
        SELECT u.* FROM users u
        WHERE lower(u.name) LIKE lower(concat('%', :name, '%'))
        """, nativeQuery = true)
```

### 1.7 Streams

- Use para transformação declarativa; use `for` quando for mais legível.
- Nunca cause efeito colateral dentro de `map`/`filter` (nada de `save()` no meio).
- `toList()` (Java 16+) em vez de `collect(Collectors.toList())`.
- Sem stream aninhado de 3 níveis: extraia método com nome de intenção.
- Cuidado com N+1: stream sobre coleções lazy (`user.getRoles()`, `user.getAddresses()`)
  dispara consultas — resolva com `join fetch` no repository.

### 1.8 Outros recursos

| Recurso | Uso no projeto |
|---------|----------------|
| `LocalDateTime` / `LocalDate` | sempre; `java.util.Date` e `Calendar` são proibidos |
| `BigDecimal` | qualquer valor monetário ou percentual; `double`/`float` proibidos nesses casos |
| `List.of`, `Map.of`, `Set.of` | coleções imutáveis de constantes |
| `String.formatted()` | formatação simples |
| `Objects.requireNonNull` | pré-condição de argumento |
| Virtual Threads | não usar sem necessidade medida e decisão registrada |
| Reflection | proibido em código de negócio |

---

## 2. Clean Code

### 2.1 Métodos

- **Um nível de abstração por método.** Se um método mistura orquestração e detalhe,
  extraia.
- **Alvo: até ~20 linhas.** Acima disso, quase sempre há um método escondido.
- **Máximo 3 parâmetros.** Mais que isso → agrupe em um `record` de parâmetro.
- **Sem parâmetro booleano de controle** (`save(user, true)`): crie dois métodos com nome
  próprio.
- **Sem flag de saída dupla:** um método consulta **ou** modifica, nunca os dois.
- **Early return** em vez de aninhamento profundo (máximo 2 níveis de indentação em lógica).
- **Nunca retorne `null`** de coleção: retorne `List.of()`.

```java
// ⛔
public void register(UserRegistrationRequest request) {
    if (request != null) {
        if (!userRepository.existsByEmail(request.email())) {
            if (!userRepository.existsByLogin(request.login())) {
                if (!request.roles().contains(RoleName.ROLE_ADMIN)) {
                    // ... 30 linhas
                }
            }
        }
    }
}

// ✅
@Transactional
public UserResponse register(UserRegistrationRequest request) {
    ensureSelfRegistrationDoesNotRequestAdmin(request.roles());

    Email email = new Email(request.email());
    ensureEmailIsUnique(email);
    ensureLoginIsUnique(request.login());

    User user = userMapper.fromRequest(request);
    user.setEmail(email.value());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setRoles(resolveRoles(request.roles()));

    return userMapper.toResponse(userRepository.save(user));
}
```

### 2.2 Comentários

- Código expressivo > comentário. Nome de método é o comentário.
- Comente **o porquê**, nunca o quê. Regra de negócio não óbvia, decisão contraintuitiva,
  workaround com link — sim. `// incrementa i` — não.
- **Proibido:** código comentado (o Git guarda), `TODO` sem issue, cabeçalho com autor/data,
  Javadoc que repete a assinatura.
- Javadoc é obrigatório apenas em: exceções de domínio, VOs de valor (invariante) e métodos
  públicos de service com regra não evidente.

### 2.3 Classes

- **SRP na prática:** se a descrição da classe precisa de "e", ela tem duas
  responsabilidades.
- Campos `private final`; injeção **por construtor**.
- Sem estado mutável compartilhado em bean singleton.
- Constantes locais `private static final`, não números mágicos:

```java
private static final int MIN_PASSWORD_LENGTH = 8;
private static final String SYSTEM_AUDITOR = "system";
```

### 2.4 Tratamento de erros

- Exceção específica de domínio, nunca `RuntimeException`/`Exception` genérica.
- **Nunca** engula exceção: `catch (Exception e) {}` é proibido.
- Mensagem útil e em português, com o dado relevante — **sem vazar senha, hash ou token**.
- Sem controle de fluxo por exceção (não use exceção como `if`).
- Não use `catch` para exceção de domínio dentro do service: deixe subir para o
  `GlobalExceptionHandler`.

### 2.5 Formatação

- 4 espaços; linha ≤ 120 colunas; sem tab.
- Ordem dentro da classe: constantes → campos → construtor → métodos públicos → privados.
- Sem linha em branco dupla; sem import `*`; sem import não usado.
- Uma declaração por linha.

---

## 3. SOLID aplicado ao Corretoria

### S — Single Responsibility

Cada classe tem um motivo para mudar.

- `UserService` cuida do cadastro e do ciclo de vida do usuário.
- `AuthenticationService` cuida de validar credenciais e emitir token. **Não** funda os dois
  porque "é tudo usuário": eles mudam por razões diferentes (regra de cadastro × política de
  autenticação).
- Normalização de texto é responsabilidade própria → `TextUtils`.
- Resolução do auditor é responsabilidade própria → `SpringSecurityAuditorAware`.
- Sinais de violação: service com >~7 dependências injetadas, arquivo com >~300 linhas,
  método privado que não usa nenhum campo da classe.

### O — Open/Closed

Aberto para extensão, fechado para modificação.

```java
// ✅ nova política de senha entra como nova implementação, sem editar o service
public interface PasswordPolicy {
    void validate(String rawPassword);
}

@Service
class PasswordPolicyChecker {
    private final List<PasswordPolicy> policies;   // Spring injeta todas

    PasswordPolicyChecker(List<PasswordPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    void validate(String rawPassword) {
        policies.forEach(policy -> policy.validate(rawPassword));
    }
}
```

Sinal de violação: `switch`/`if` sobre tipo que cresce a cada nova regra de negócio.

### L — Liskov Substitution

Subtipo não pode surpreender quem usa o supertipo.

- Não sobrescreva método para lançar `UnsupportedOperationException`.
- Não fortaleça pré-condição nem enfraqueça pós-condição na subclasse.
- No JPA: entidade filha não relaxa invariante da base `Auditable`.
- Prefira **composição** a herança de conveniência (herdar só para reusar código é violação
  disfarçada).

### I — Interface Segregation

Cliente não depende de método que não usa.

- Repository expõe só o que o service precisa — nada de "repository genérico" com 20
  métodos.
- Não crie `UserFacade` com todas as operações; separe por caso de uso.
- Beans de segurança são pequenos e específicos: `UserSecurity.isSelf` faz uma coisa.

### D — Dependency Inversion

Depender de abstração, não de detalhe.

```java
// ✅ injeção por construtor, campos final, sem @Autowired
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }
}
```

- **Proibido:** `new` de dependência com comportamento dentro do service, `@Autowired` em
  campo, injeção por setter, `static` mutável, service chamando `LocalDateTime.now()`
  diretamente quando a data é regra de negócio testável (injete um `Clock` — caso do
  vencimento de `PasswordResetToken`).

---

## 4. Regras específicas do domínio Corretoria

Estas não são estilo — são corretude:

1. **Identidade e posse vêm do token, nunca do payload.** O id do usuário afetado em uma
   operação de posse é validado contra o autenticado (`UserSecurity.isSelf`). Confiar em id
   do corpo é falha de autorização (*IDOR*).
2. **Autocadastro nunca concede `ROLE_ADMIN`.** O `register` rejeita explicitamente
   (`ForbiddenOperationException`), impedindo escalonamento de privilégio.
3. **Senha só existe como hash BCrypt.** Nunca logue, retorne ou compare senha em texto
   plano; comparação sempre por `passwordEncoder.matches`.
4. **`Response` nunca carrega senha, hash ou token** — exceto `AuthResponse`, que carrega o
   JWT emitido.
5. **E-mail normalizado para minúsculas antes de qualquer checagem de unicidade** (VO
   `Email`); CEP normalizado para 8 dígitos (VO `ZipCode`).
6. **Nada de `ROLE_OWNER`.** O papel do corretor é `ROLE_REALTOR`; `owner` não é sinônimo
   aceitável.
7. **Gestão de usuários é do admin.** Listagem e busca exigem `ROLE_ADMIN`; não implemente
   atalho que exponha coleção de usuários a papel comum.

---

## 5. Testes (parte do padrão, não opcional)

Toda regra de negócio nova nasce com teste.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("register - papel ADMIN solicitado - lança ForbiddenOperationException")
        void shouldRejectRegistrationWhenRoleAdminIsRequested() {
            // arrange
            UserRegistrationRequest request = aRequestWithRoles(RoleName.ROLE_ADMIN);

            // act + assert
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("ROLE_ADMIN");
            then(userRepository).shouldHaveNoInteractions();
        }
    }
}
```

Regras: um comportamento por teste · AssertJ (`assertThat`) · BDD Mockito
(`given`/`willReturn`/`then`) · `@Nested` por método · `@DisplayName` em português · sem
lógica condicional no teste · cobrir o caminho triste, não só o feliz · sem `Thread.sleep`.

---

## 6. Checklist antes de entregar código

**Java 21**
- [ ] VO é `record`; imutável; validado no construtor compacto
- [ ] `switch` de expressão (sem `break`) sobre enum
- [ ] `Optional` só como retorno; sem `.get()` desprotegido
- [ ] `LocalDateTime` em data; `BigDecimal` em valor
- [ ] Sem reflection, sem `java.util.Date`, sem `Collectors.toList()`

**Clean Code**
- [ ] Método ≤ ~20 linhas, um nível de abstração, ≤ 3 parâmetros
- [ ] Sem parâmetro booleano de controle; consulta ≠ comando
- [ ] Early return; ≤ 2 níveis de indentação
- [ ] Zero número mágico, zero código comentado, zero `catch` vazio
- [ ] Exceção de domínio específica com mensagem útil em português
- [ ] Nunca retorna `null` de coleção

**SOLID**
- [ ] Uma responsabilidade por classe (descrição sem "e")
- [ ] Extensão por nova implementação, não por editar `switch`
- [ ] Injeção por construtor, campos `private final`, sem `@Autowired` em campo
- [ ] Interfaces enxutas; nenhum método não usado pelo cliente

**Domínio**
- [ ] Posse validada contra o usuário autenticado, não contra id do payload
- [ ] Autocadastro rejeita `ROLE_ADMIN`
- [ ] Senha só como hash; `Response` sem dado sensível
- [ ] E-mail e CEP normalizados antes da persistência
- [ ] Nenhuma ocorrência de `ROLE_OWNER`

**Testes**
- [ ] Caminho feliz + triste cobertos
- [ ] AssertJ + BDD Mockito + `@Nested` + `@DisplayName`

**Consistência**
- [ ] Nomes conforme `corretoria-naming-standards`
- [ ] Camadas conforme `corretoria-architecture-guard`

---

## 7. Formato do parecer de código

```
### Parecer de código

**Conforme**
- (o que já está certo — reconheça, não só aponte erro)

**Ajustes necessários**
| # | Onde | Problema | Princípio | Correção |
|---|------|----------|-----------|----------|
| 1 | `UserService.register` | 4 níveis de aninhamento | Clean Code §2.1 | extrair `ensure...` com early return |
| 2 | `UserResponse` | expõe campo `password` | Domínio §4.4 | remover do record |

**Sugestões (não bloqueantes)**
- (melhorias opcionais)

**Código revisado**
(entregue o código já corrigido, não apenas a lista)
```

Se o código estiver conforme, diga isso sem inventar ajuste cosmético.
