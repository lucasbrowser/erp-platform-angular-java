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

Na Fase 1, permissões por operação ficaram pendentes de validação de negócio; a Etapa 3.1 aplicou no catálogo o menor privilégio já documentado, mantendo a revisão da matriz para módulos futuros.

## Fase 1.2 — especificação consolidada e OIDC

- [x] Auditar documentação anterior e registrar escopo confirmado: uma empresa, estoque negativo proibido, baixas manuais, sem fiscal/bancos automáticos/multi-tenancy.
- [x] Definir perfis iniciais e matriz de permissões de menor privilégio.
- [x] Versionar realm `erp-core` de desenvolvimento, client web PKCE S256 e audiência da API.
- [x] Validar access tokens no Resource Server por assinatura, issuer, expiração e audiência; mapear roles conhecidas.
- [x] Cobrir 401, 403, issuer, audiência e conversão de roles com JWTs assinados de teste.
- [ ] Conferir settings efetivos do client/mapper importados e validar access token real emitido pelo realm.
- [ ] Especificar e implementar papéis por operação nos módulos futuros após aprovação de stakeholders.

## Etapa 3.1.1 — estabilização do catálogo

- [x] Validar `api-contract.yaml` semanticamente como OpenAPI 3.1 e fixar essa validação na suíte Maven.
- [x] Conferir operações, filtros, paginação, segurança, schemas e respostas do contrato contra controllers e DTOs.
- [x] Documentar que atualizações não alteram `active` e que reativação requer decisão funcional explícita.
- [x] Registrar procedimento reproduzível de smoke OIDC usando somente contas locais legítimas existentes.
- [ ] Executar Authorization Code + PKCE com conta SALES/STOCK_OPERATOR existente e validar `/me` e leitura de catálogo com access token real.
- [ ] Executar leitura negada (403) com conta existente sem role de catálogo, se disponível; não criar contas ou alterar atribuições para este smoke.

Na conclusão da Fase 1, não iniciar módulos funcionais. A autorização posterior da Etapa 3.1 iniciou somente o catálogo backend.

## Etapa 3.1 — backend do catálogo

- [x] Criar migration incremental para categorias e produtos, com FK, unicidade, validações e índices.
- [x] Implementar APIs paginadas de categorias e produtos com DTOs, validação e erros Problem Details.
- [x] Aplicar permissões de catálogo no servidor conforme matriz: leitura ADMIN/MANAGER/SALES/STOCK_OPERATOR; escrita ADMIN/MANAGER.
- [x] Cobrir persistência PostgreSQL, regras e restrições de banco com testes de integração.
- [x] Executar validação final e revisar o contrato OpenAPI atualizado.
- [ ] Revisar política de reativação de cadastros com stakeholders antes de criar endpoints de ativação.

Produtos, estoque, vendas e financeiro permanecem fora desta etapa.

## Etapa 3.2.1 — integração Angular com Keycloak

- [x] Configurar adapter oficial OIDC, PKCE S256, sessão e rotas protegidas sem tokens persistidos.
- [x] Restringir o interceptor bearer à origem e caminho da API; manter endpoint de autenticação sem login automático em 401.
- [x] Registrar fluxo manual local e estado de validação real.
- [ ] Executar lint, testes e build Angular em Node 22.22.3/npm 11.6.2; o runtime não estava disponível no ambiente desta execução.
- [ ] Autenticar usuário legítimo existente no navegador e chamar API com access token real; discovery local sozinha não comprova login nem audience.
