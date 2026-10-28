# ADR-0002: PostgreSQL e Flyway

- Status: Aceita
- Data: 2026-10-28

## Contexto

É necessário banco relacional transacional, com histórico explícito de alterações de esquema.

## Decisão

Usar PostgreSQL 17.11 na infraestrutura local e Flyway via integração gerenciada pelo Spring Boot 3.5.16. A versão 17 é o limite atualmente suportado pela versão de Flyway gerenciada pelo Spring Boot. Migrações ficam em `apps/api/src/main/resources/db/migration`, são SQL versionado e append-only. Nesta fase há apenas migração sem alterações de esquema.

## Consequências

Esquema é reproduzível e o histórico fica auditável. Mudanças de esquema seguem estratégia expand/contract quando houver deploy concorrente. Desenvolvimento local usa um banco compartilhado com database separado para Keycloak.
