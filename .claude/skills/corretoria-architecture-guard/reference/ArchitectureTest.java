package com.corretoria.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(
        packages = "com.corretoria",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String CONTROLLER = "..controller..";
    private static final String SERVICE = "..service..";
    private static final String REPOSITORY = "..repository..";
    private static final String ENTITY = "..entity..";
    private static final String ENUMS = "..enums..";
    private static final String MAPPER = "..mapper..";
    private static final String VO = "..vo..";
    private static final String UTIL = "..util..";
    private static final String SECURITY = "..security..";

    // ---------------------------------------------------------------- camadas

    @ArchTest
    static final ArchRule respeitaAArquiteturaEmCamadas = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Apresentacao").definedBy(CONTROLLER)
            .layer("Negocio").definedBy(SERVICE)
            .layer("Persistencia").definedBy(REPOSITORY)
            .layer("Dominio").definedBy(ENTITY, ENUMS)
            .whereLayer("Apresentacao").mayNotBeAccessedByAnyLayer()
            .whereLayer("Negocio").mayOnlyBeAccessedByLayers("Apresentacao")
            .whereLayer("Persistencia").mayOnlyBeAccessedByLayers("Negocio")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Negocio", "Persistencia")
            .as("A regra de dependência é controller -> service -> repository -> entity; "
                    + "nenhuma camada pode ser pulada nem invertida");

    @ArchTest
    static final ArchRule controllerNaoAcessaRepository = noClasses()
            .that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAPackage(REPOSITORY)
            .as("O controller não pode acessar o repository: a regra de negócio nasceria na "
                    + "camada de apresentação e se duplicaria. Crie um método no service e delegue");

    @ArchTest
    static final ArchRule serviceNaoDependeDeController = noClasses()
            .that().resideInAPackage(SERVICE)
            .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
            .as("O service não pode depender do controller: isso inverte o fluxo e acopla a "
                    + "regra de negócio ao transporte HTTP");

    @ArchTest
    static final ArchRule repositoryNaoDependeDeCamadasSuperiores = noClasses()
            .that().resideInAPackage(REPOSITORY)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, VO)
            .as("O repository não conhece camadas superiores: pode depender apenas de entity e enums");

    // ---------------------------------------------------------------- domínio

    @ArchTest
    static final ArchRule entityNaoDependeDeNinguemAcima = noClasses()
            .that().resideInAPackage(ENTITY)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, REPOSITORY, VO, MAPPER)
            .as("O domínio fica no centro: a entidade não aponta para service, controller, "
                    + "repository, VO nem mapper. Mova a conversão para o mapper");

    @ArchTest
    static final ArchRule entityNaoUsaAnotacaoDeApi = noClasses()
            .that().resideInAPackage(ENTITY)
            .should().dependOnClassesThat()
            .resideInAnyPackage("io.swagger..", "com.fasterxml.jackson..", "jakarta.validation..")
            .as("A entidade não carrega anotação de API (@Schema, @JsonProperty) nem validação de "
                    + "contrato: isso pertence ao VO, que é a fronteira da API");

    // ---------------------------------------------------------------- contrato

    @ArchTest
    static final ArchRule voNaoDependeDeEntity = noClasses()
            .that().resideInAPackage(VO)
            .should().dependOnClassesThat().resideInAnyPackage(ENTITY, REPOSITORY, SERVICE, CONTROLLER)
            .as("O VO é contrato puro: não conhece entidade nem persistência");

    @ArchTest
    static final ArchRule todoVoEhRecord = classes()
            .that().resideInAPackage(VO)
            .should().beRecords()
            .as("Todo VO é um record imutável (Java 21): sem setter e sem construtor vazio");

    @ArchTest
    static final ArchRule controllerNaoRetornaEntity = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER)
            .should().haveRawReturnType(resideInAPackage(ENTITY))
            .as("O controller devolve somente VO (ou EntityModel/PagedModel sobre VO): devolver "
                    + "entidade vaza o schema e a senha para a API");

    @ArchTest
    static final ArchRule controllerNaoRecebeEntity = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER)
            .should().haveRawParameterTypes(
                    com.tngtech.archunit.base.DescribedPredicate.anyElementThat(resideInAPackage(ENTITY)))
            .as("O controller recebe somente VO: entidade JPA não cruza a fronteira da API");

    @ArchTest
    static final ArchRule responseNaoExpoeDadoSensivel = noFields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Response")
            .and().areDeclaredInClassesThat().doNotHaveSimpleName("AuthResponse")
            .should().haveNameMatching("(?i).*(password|senha|hash|secret|token).*")
            .as("Nenhum Response expõe senha, hash ou token; a única exceção é AuthResponse, "
                    + "que carrega o JWT emitido");

    // ------------------------------------------------------------ persistência

    @ArchTest
    static final ArchRule repositoryEhInterface = classes()
            .that().resideInAPackage(REPOSITORY)
            .should().beInterfaces()
            .andShould().beAssignableTo(JpaRepository.class)
            .as("Repository é interface estendendo JpaRepository, nunca classe concreta");

    // ------------------------------------------------------------- transporte

    @ArchTest
    static final ArchRule negocioEDominioSemTransporte = noClasses()
            .that().resideInAnyPackage(SERVICE, ENTITY, REPOSITORY, MAPPER)
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
            .as("Service, entity, repository e mapper não conhecem HTTP: nada de "
                    + "HttpServletRequest, ResponseEntity ou @RequestMapping fora do controller");

    // -------------------------------------------------------------- transação

    @ArchTest
    static final ArchRule transacaoSomenteNoService = classes()
            .that().areAnnotatedWith(Transactional.class)
            .or().containAnyMethodsThat(
                    com.tngtech.archunit.base.DescribedPredicate.describe(
                            "são anotados com @Transactional",
                            method -> method.isAnnotatedWith(Transactional.class)))
            .should().resideInAPackage(SERVICE)
            .as("A fronteira transacional é o service: @Transactional em controller ou "
                    + "repository indica regra de negócio na camada errada");

    // ---------------------------------------------------------------- injeção

    @ArchTest
    static final ArchRule semAutowiredEmCampo = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .as("Injeção é por construtor com campos final: @Autowired em campo esconde a "
                    + "dependência e impede o teste sem contexto Spring");

    @ArchTest
    static final ArchRule camposDeDependenciaSaoFinal = fields()
            .that().areDeclaredInClassesThat().resideInAnyPackage(SERVICE, CONTROLLER, SECURITY, MAPPER)
            .and().areNotStatic()
            .should().beFinal()
            .as("Campos de dependência são private final, atribuídos no construtor");

    // ------------------------------------------------------------- utilitário

    @ArchTest
    static final ArchRule utilNaoDependeDoProjeto = noClasses()
            .that().resideInAPackage(UTIL)
            .should().dependOnClassesThat(
                    resideInAPackage("com.corretoria..").and(not(resideInAPackage(UTIL))))
            .as("Utilitário é folha: com.corretoria.util não depende de nenhum outro pacote do "
                    + "projeto, evitando ciclo");

    // ------------------------------------------------------------------ ciclos

    @ArchTest
    static final ArchRule semCiclosEntrePacotes = slices()
            .matching("com.corretoria.(*)..")
            .should().beFreeOfCycles()
            .as("Nenhum ciclo entre pacotes: ciclo é erosão arquitetural garantida");

    // ------------------------------------------------------------ nomenclatura

    @ArchTest
    static final ArchRule controllerNoPacoteController = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage(CONTROLLER)
            .as("Classe terminada em Controller mora em com.corretoria.controller");

    @ArchTest
    static final ArchRule serviceNoPacoteService = classes()
            .that().haveSimpleNameEndingWith("Service")
            .should().resideInAnyPackage(SERVICE, SECURITY)
            .as("Classe terminada em Service mora em com.corretoria.service "
                    + "(exceto os *UserDetailsService/JwtService, que são de segurança)");

    @ArchTest
    static final ArchRule repositoryNoPacoteRepository = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage(REPOSITORY)
            .as("Classe terminada em Repository mora em com.corretoria.repository");

    @ArchTest
    static final ArchRule mapperNoPacoteMapper = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().resideInAPackage(MAPPER)
            .as("Classe terminada em Mapper mora em com.corretoria.mapper");

    @ArchTest
    static final ArchRule entidadesNoPacoteEntity = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(ENTITY)
            .as("Entidade JPA mora em com.corretoria.entity");

    @ArchTest
    static final ArchRule requestEResponseNoPacoteVo = classes()
            .that().haveSimpleNameEndingWith("Request").or().haveSimpleNameEndingWith("Response")
            .should().resideInAPackage(VO)
            .as("VO de contrato mora em com.corretoria.vo.v1.request/response");

    @ArchTest
    static final ArchRule excecaoNoPacoteException = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .should().resideInAPackage("..exception..")
            .as("Exceção de domínio mora em com.corretoria.exception");

    @ArchTest
    static final ArchRule semPrefixoIEmInterface = noClasses()
            .that().areInterfaces()
            .should().haveNameMatching(".*\\.I[A-Z][a-zA-Z]*")
            .as("Interface não recebe prefixo I: use UserService, não IUserService");

    @ArchTest
    static final ArchRule semDtoNoNome = noClasses()
            .should().haveSimpleNameContaining("DTO")
            .as("O projeto não usa a nomenclatura DTO: objetos de fronteira são VOs "
                    + "(sufixo Request/Response)");

    @ArchTest
    static final ArchRule semSufixosProibidos = noClasses()
            .that().resideInAPackage("com.corretoria..")
            .should().haveSimpleNameEndingWith("Helper")
            .orShould().haveSimpleNameEndingWith("Manager")
            .orShould().haveSimpleNameEndingWith("Processor")
            .as("Sufixos Helper, Manager e Processor não descrevem responsabilidade: "
                    + "nomeie pela intenção");

    // ------------------------------------------------------------- segurança

    @ArchTest
    static final ArchRule contextoDeSegurancaSoEmSecurityEService = noClasses()
            .that().resideOutsideOfPackages(SECURITY, SERVICE, "..config..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
            .as("A identidade autenticada é resolvida em security ou service: ler o "
                    + "SecurityContextHolder no controller espalha a regra de posse");

    @ArchTest
    static final ArchRule securityNaoDependeDeController = noClasses()
            .that().resideInAPackage(SECURITY)
            .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
            .as("A segurança é transversal e fica abaixo da apresentação: pode ler o "
                    + "repository para resolver o usuário autenticado, mas nunca depende de "
                    + "controller");

    @ArchTest
    static final ArchRule semAnotacaoDeAutorizacaoForaDoController = noClasses()
            .that().resideInAnyPackage(ENTITY, REPOSITORY, VO)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security..")
            .as("Entidade, repository e VO não conhecem Spring Security");
}
