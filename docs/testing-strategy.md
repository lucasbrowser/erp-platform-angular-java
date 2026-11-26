# Estratégia de testes

## Princípios

Use testes unitários para lógica isolada e testes de integração para contratos entre Spring, JPA, Flyway e PostgreSQL. A quantidade de testes não substitui cobertura dos comportamentos de risco. Testes precisam ser determinísticos, isolados e executados pelo comando suportado do projeto.

Uma etapa só é validada quando compilação e verificações obrigatórias foram executadas com sucesso, não há falhas/erros nem testes indevidamente ignorados, e o banco usado foi identificado como banco dedicado de testes. Inspeção estática não equivale a execução.

## Backend

- Java 21, JUnit Jupiter e Spring Boot Test são fornecidos pelo `spring-boot-starter-test`; AssertJ é usado nas asserções. Os testes HTTP existentes usam MockMvc e `spring-security-test`.
- `@SpringBootTest` exercita o contexto real. Testes de persistência e consulta usam PostgreSQL real, migrations Flyway e validação Hibernate; não substituir por H2.
- A revisão do projeto não encontrou Testcontainers nem uso direto de Mockito. Não adicione dependências ou infraestrutura de teste redundante sem necessidade demonstrada.
- Use fixtures válidos segundo validadores de domínio, valores únicos quando houver índice unique e dados criados por teste. Não dependa da ordem dos testes. Use rollback/limpeza isolada conforme o padrão da classe.
- Testes de segurança cobrem ausência/invalidez de token, issuer/audience, roles e respostas 401/403; mocks de autenticação não comprovam login real no Keycloak.
- Testes OpenAPI validam o YAML e seus paths/schemas contra as APIs implementadas; não confundir contrato documentado de módulo futuro com endpoint existente.

### Banco de teste

A configuração `application-test.yml` exige as três variáveis abaixo e não usa fallback automático para `DATABASE_URL`:

```powershell
$env:TEST_DATABASE_URL = "jdbc:postgresql://localhost:5433/erp_core_test"
$env:TEST_DATABASE_USERNAME = "<usuario-do-banco-de-teste>"
$env:TEST_DATABASE_PASSWORD = "<senha-do-banco-de-teste>"
```

Use somente um banco exclusivo e dispensável chamado `erp_core_test`. Não use o banco `erp_core` do Compose, produção ou banco que contenha dados a preservar. O Compose atual não cria `erp_core_test`; confira `apps/api/src/test/resources/README.md`, confirme o destino da URL antes de executar e disponibilize PostgreSQL real.

Comandos Windows a partir da raiz:

```powershell
mvn.cmd -f apps/api/pom.xml test
mvn.cmd -f apps/api/pom.xml verify
```

Para uma classe específica, use `mvn.cmd -f apps/api/pom.xml "-Dtest=NomeDaClasse" test`. Não informe sucesso se compilação, banco, Flyway ou Hibernate validation não executaram. Use PowerShell nativo; não invoque binários Windows por WSL.

## Frontend

- O executor é `@angular/build:unit-test`, em `apps/web/angular.json`, com `tsconfig.spec.json`. Angular usa o ambiente de teste configurado pelo builder; não executar Vitest diretamente como substituto.
- Dependências incluem Angular Testing/CLI, Vitest e jsdom; o script `test` já define `ng test --watch=false`.
- Node e npm requeridos constam de `apps/web/.nvmrc` e `package.json` (`Node 22.22.3`, npm 11.x, packageManager fixado em 11.6.2). Confirme versões efetivas antes da validação.
- Scripts disponíveis em `apps/web/package.json`: `npm.cmd test`, `npm.cmd run lint`, `npm.cmd run build`, além de `start` e `format`. Não invente scripts nem atualize lockfile incidentalmente.

Execute os comandos na pasta `apps/web`:

```powershell
npm.cmd test
npm.cmd run lint
npm.cmd run build
```

## Relatório

Registre nomes/comandos, ambiente, quantidade de testes quando fornecida pelo runner, falhas/erros/ignorados, status de lint/build, banco configurado e limitações. Relate separadamente etapas não executadas; nunca as descreva como aprovadas.
