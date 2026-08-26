package com.corretoria.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI da API.
 *
 * <p>O esquema de segurança declarado aqui é o que habilita o botão <strong>Authorize</strong>
 * no Swagger UI: sem ele, a interface até lista os endpoints protegidos, mas não permite
 * executá-los, porque não há onde informar o token.
 *
 * <p>A exigência de token é declarada <strong>por operação</strong>, e não globalmente, para
 * que o cadeado apareça apenas onde ele existe de fato — o login e o autocadastro são
 * públicos, e marcá-los como protegidos seria documentar errado.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Corretoria API",
                version = "1.1.0",
                description = """
                        Plataforma que reúne corretores, clientes e administração sob uma \
                        identidade e um controle de acesso únicos, com catálogo de anúncios \
                        de imóveis e automóveis.

                        **A leitura do catálogo é pública:** `GET /api/v1/listings` e \
                        `GET /api/v1/listings/{id}` funcionam sem token. Publicar exige \
                        `ROLE_REALTOR`; editar, pausar e mexer nas fotos exige ser o dono do \
                        anúncio ou administrador.

                        **Como autenticar:** chame `POST /api/v1/auth/login` com um dos \
                        usuários de demonstração, copie o `token` da resposta, clique em \
                        **Authorize** e cole o token.

                        | Login | Senha | Papel |
                        |-------|-------|-------|
                        | `corretor.demo` | `corretor12345` | `ROLE_REALTOR` |
                        | `cliente.demo` | `cliente12345` | `ROLE_CUSTOMER` |
                        | `admin.demo` | `admin12345` | `ROLE_ADMIN` |

                        Os erros seguem o formato ProblemDetail (RFC 7807): o campo `type` \
                        identifica o problema e é o que distingue erros que compartilham o \
                        mesmo status.""",
                contact = @Contact(name = "Felipe Dias Mac Dowell")),
        servers = @Server(url = "/", description = "Servidor atual"),
        tags = {
                @Tag(name = "Autenticação", description = "Emissão de token JWT"),
                @Tag(name = "Usuários", description = "Cadastro e gestão de usuários"),
                @Tag(name = "Anúncios",
                        description = "Publicação e busca de anúncios de imóveis e automóveis"),
                @Tag(name = "Buscas salvas",
                        description = "Alerta de anúncio novo e vitrine de anúncios de procura"),
                @Tag(name = "Notificações",
                        description = "Avisos in-app do usuário autenticado"),
                @Tag(name = "Propostas",
                        description = "Negociação entre cliente e corretor")
        })
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Informe apenas o token; o prefixo Bearer é acrescentado pelo Swagger UI.")
public class OpenApiConfig {

    /** Nome do esquema de segurança, referenciado nos endpoints protegidos. */
    public static final String BEARER_AUTH = "bearerAuth";
}
