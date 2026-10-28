# Requisitos

## Funcionais (produto futuro)

- RF-01: manter cadastros mestres conforme módulos aprovados pelo negócio.
- RF-02: permitir que usuários autenticados executem somente operações autorizadas.
- RF-03: registrar autoria e momento das alterações relevantes.
- RF-04: expor recursos da API sob `/api/v1` com contratos versionados.
- RF-05: inicializar e atualizar o esquema por migrações versionadas.

RF-01 a RF-05 são requisitos de direção; não autorizam implementar funcionalidades de negócio nesta fase. O contrato e a API entregues limitam-se a infraestrutura e health.

## Não funcionais

- RNF-01: Java 21, Spring Boot, Angular/TypeScript, PostgreSQL e Flyway em versões explicitamente documentadas.
- RNF-02: monólito modular; dependências entre módulos devem apontar para interfaces públicas.
- RNF-03: autenticação de API por OAuth 2.0/OIDC bearer JWT via Keycloak.
- RNF-04: credenciais, chaves e tokens reais nunca entram no Git.
- RNF-05: CI executa compilação, testes, lint e validação de migrações.
- RNF-06: endpoints operacionais limitados a health/info; detalhes de ambiente não públicos.
- RNF-07: execução local reproduzível via Docker Compose.
- RNF-08: migrações são append-only após compartilhadas.
- RNF-09: segredos de produção devem vir de secret manager do ambiente de implantação.

## Critérios de aceite da Fase 1

- Estrutura documentada e modular criada, sem regras de negócio.
- Backend compila/testa e executa migração inicial em PostgreSQL.
- Frontend tem build, lint e teste configurados.
- Compose liga PostgreSQL e Keycloak em portas de loopback.
- CI executa as verificações e versões/ações fixadas estão registradas.
- Resultados locais e limitações estão documentados com evidência real.
