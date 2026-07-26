# Evidências da matriz 15.2

Coletadas em 2026-07-26 12:06:55 contra a pilha subida do zero com
`docker compose down -v && docker compose up --build`.

Cada linha tem a transcrição completa no arquivo `<id>-<slug>.txt`: requisição,
cabeçalho `Authorization` (token truncado), corpo enviado, status e corpo da resposta.

| # | Cenário | Esperado | Obtido |
|---|---------|---------:|--------|
| `A1` | login-corretor-valido | 200 | ✅ 200 |
| `A2` | login-cliente-valido | 200 | ✅ 200 |
| `A3` | login-admin-valido | 200 | ✅ 200 |
| `A4` | login-senha-incorreta | 401 | ✅ 401 |
| `A5` | login-inexistente | 401 | ✅ 401 |
| `A6` | login-campos-ausentes | 400 | ✅ 400 |
| `U1` | cadastro-corretor-201 | 201 | ✅ 201 |
| `U2` | cadastro-cliente-201 | 201 | ✅ 201 |
| `U3` | cadastro-campos-ausentes-400 | 400 | ✅ 400 |
| `U4` | email-formato-invalido-400 | 400 | ✅ 400 |
| `U4b` | email-passa-bean-falha-vo-400 | 400 | ✅ 400 |
| `U5` | cep-formato-invalido-400 | 400 | ✅ 400 |
| `U6` | email-ja-cadastrado-409 | 409 | ✅ 409 |
| `U7` | email-duplicado-so-na-caixa-409 | 409 | ✅ 409 |
| `U8` | login-ja-cadastrado-409 | 409 | ✅ 409 |
| `U9` | autocadastro-role-admin-403 | 403 | ✅ 403 |
| `C1` | consulta-proprio-200 | 200 | ✅ 200 |
| `C2` | consulta-de-outro-403 | 403 | ✅ 403 |
| `C3` | consulta-via-admin-200 | 200 | ✅ 200 |
| `C4` | consulta-id-inexistente-404 | 404 | ✅ 404 |
| `C5` | sem-cabecalho-authorization-401 | 401 | ✅ 401 |
| `C6` | token-malformado-401 | 401 | ✅ 401 |
| `C7` | listagem-paginada-admin-200 | 200 | ✅ 200 |
| `C8` | listagem-usuario-comum-403 | 403 | ✅ 403 |
| `C9` | busca-por-nome-admin-200 | 200 | ✅ 200 |
| `C10` | busca-sem-resultado-200 | 200 | ✅ 200 |
| `C11` | paginacao-ordenacao-hateoas-200 | 200 | ✅ 200 |
| `M1` | atualizacao-do-proprio-200 | 200 | ✅ 200 |
| `M2` | atualizacao-validacao-400 | 400 | ✅ 400 |
| `M4` | atualizacao-de-outro-403 | 403 | ✅ 403 |
| `M5` | atualizacao-inexistente-404 | 404 | ✅ 404 |
| `P2` | senha-atual-incorreta-400 | 400 | ✅ 400 |
| `P3` | confirmacao-divergente-400 | 400 | ✅ 400 |
| `P4` | senha-de-outro-usuario-403 | 403 | ✅ 403 |
| `P1` | troca-de-senha-204 | 204 | ✅ 204 |
| `P5` | login-com-a-senha-nova-200 | 200 | ✅ 200 |
| `P5b` | login-com-a-senha-antiga-401 | 401 | ✅ 401 |
| `D2` | exclusao-de-outro-403 | 403 | ✅ 403 |
| `D3` | exclusao-inexistente-404 | 404 | ✅ 404 |
| `D1` | exclusao-do-proprio-204 | 204 | ✅ 204 |
| `D4` | consulta-ao-excluido-404 | 404 | ✅ 404 |

**Resultado:** 41 conforme, 0 divergentes.
