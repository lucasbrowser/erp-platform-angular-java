# ADR-0003: OIDC com Keycloak

- Status: Aceita como direção; realm pendente
- Data: 2026-10-28

## Contexto

O backend precisa validar identidade sem armazenar senhas ou acoplar autenticação de usuário à lógica de negócio.

## Decisão

Keycloak 26.7.3 será o provedor OIDC. Spring Security Resource Server valida bearer JWT e issuer; a API não emite tokens. O Compose executa Keycloak somente para desenvolvimento local. Realm, client, mapeamento de roles/claims e jornada de autenticação ficam para etapa posterior.

## Consequências

Identidade fica externalizada e é possível centralizar SSO. Em produção, tokens e issuer exigem configuração segura e verificação de assinatura/issuer. O endpoint JWKS local não estará disponível até provisionar o realm.
