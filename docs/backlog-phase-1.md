# Backlog — Fase 1: Fundação técnica

- [x] Inspecionar repositório e ferramentas disponíveis.
- [x] Especificar visão, requisitos, regras, domínio, segurança, API e decisões.
- [x] Criar monorepo e esqueletos backend/frontend.
- [x] Configurar PostgreSQL/Flyway, Actuator, validação, segurança base e testes.
- [x] Configurar Angular, rotas, lint, formatação e teste inicial.
- [x] Criar Compose local de PostgreSQL e Keycloak.
- [x] Criar CI com ações fixadas por SHA e verificações.
- [x] Executar lint, teste, build do frontend e gerar lockfile npm.
- [x] Executar startup real da stack local e smoke de PostgreSQL, Keycloak, API e Flyway.
- [x] Importar realm no Keycloak local e validar discovery/JWKS.
- [ ] Obter access token real via Authorization Code + PKCE e validar audiência na API.
- [ ] Obter validação de stakeholders para permissões por operação; tenancy do MVP já foi confirmada como uma única empresa.

Permissões por operação precisam de validação de negócio antes da implementação dos módulos; não fazem parte da implementação funcional desta fundação.

## Fase 1.2 — especificação consolidada e OIDC

- [x] Auditar documentação anterior e registrar escopo confirmado: uma empresa, estoque negativo proibido, baixas manuais, sem fiscal/bancos automáticos/multi-tenancy.
- [x] Definir perfis iniciais e matriz de permissões de menor privilégio.
- [x] Versionar realm `erp-core` de desenvolvimento, client web PKCE S256 e audiência da API.
- [x] Validar access tokens no Resource Server por assinatura, issuer, expiração e audiência; mapear roles conhecidas.
- [x] Cobrir 401, 403, issuer, audiência e conversão de roles com JWTs assinados de teste.
- [ ] Conferir settings efetivos do client/mapper importados e validar access token real emitido pelo realm.
- [ ] Especificar e implementar papéis por operação nos módulos futuros após aprovação de stakeholders.

Não iniciar a fase funcional (catálogo, estoque, vendas ou financeiro) neste backlog.
