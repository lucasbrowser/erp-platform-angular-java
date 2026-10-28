# Modelo de domínio inicial

O modelo desta fase é conceitual e não cria tabelas de negócio. O MVP representa **uma única empresa**; não há entidade Organização/Tenant nem coluna de tenant nos dados.

```text
Aplicação ERP Core (monólito modular)
  ├── Identidade externa (sujeito OIDC do Keycloak)
  ├── Catálogo (futuro)
  ├── Estoque (futuro; saldo nunca negativo)
  ├── Vendas (futuro; confirmação depende de estoque suficiente)
  └── Financeiro (futuro; contas a pagar/receber com baixa manual)
```

Identidade externa não equivale a cadastro de usuário de negócio. A API usa `sub` como identificador do principal OIDC e roles do realm para autorização. Não se presume cadastro local de usuário nesta fundação.

Limites de agregados, chaves, ownership, cardinalidades, auditoria, concorrência de estoque e transições de estado devem ser decididos antes da implementação dos módulos. Multi-tenancy, emissão fiscal e integrações bancárias automáticas estão fora do escopo do MVP.
