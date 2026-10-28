# ADR-0001: Monólito modular

- Status: Aceita
- Data: 2026-10-28

## Contexto

O produto necessita de uma base simples para iniciar módulos de ERP sem custo operacional de serviços distribuídos ou contratos de rede internos.

## Decisão

Entregar uma aplicação Spring Boot única, com módulos isolados por pacote (`com.erp.core.<modulo>`), dependências internas explícitas e frontend Angular único. Não criar microsserviços. Um módulo não acessa internals de outro módulo.

O MVP atende uma empresa com múltiplos usuários. Não incluir abstração ou coluna de multi-tenancy sem nova decisão aprovada.

## Consequências

Transações locais são simples e implantação é una. Limites devem ser revisados por dependências e testes arquiteturais quando houver domínio suficiente. Extração futura de serviço exige evidência e ADR próprio.
