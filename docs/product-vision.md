# Visão e escopo do produto

## Visão

ERP Core será uma plataforma web para apoiar operações administrativas de uma pequena ou média empresa, com módulos coesos sobre uma base única de dados, identidade e auditoria. O MVP atende uma empresa e seus múltiplos usuários; multi-tenancy não faz parte do escopo.

## Objetivos

- Oferecer uma base consistente para cadastros e processos operacionais.
- Preservar integridade dos dados e rastreabilidade de alterações.
- Permitir evolução por módulos dentro de um monólito modular.

## Escopo desta fase

Documentação de produto e decisões; esqueleto Angular e Spring; integração técnica PostgreSQL/Flyway; execução local com Keycloak; verificações automatizadas. Não há entidades persistidas de negócio, telas funcionais, cadastro/login customizado, produtos, estoque, vendas ou financeiro.

## Fora do escopo do MVP

Multi-tenancy, emissão fiscal, integrações bancárias automáticas, aplicativo móvel, microsserviços e alta disponibilidade distribuída. O financeiro prevê baixa manual de contas a pagar e receber.

## Usuários previstos

Usuários com perfis iniciais ADMIN, MANAGER, SALES, STOCK_OPERATOR e FINANCE. A matriz inicial de operações está em `security-model.md`; detalhes e segregação de funções devem ser validados antes da implementação funcional.
