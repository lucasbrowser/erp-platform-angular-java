# Modelo de domínio inicial

O modelo da fundação era conceitual. A Etapa 3.1 materializa o primeiro módulo de negócio, o Catálogo. O MVP representa **uma única empresa**; não há entidade Organização/Tenant nem coluna de tenant nos dados.

```text
Aplicação ERP Core (monólito modular)
  ├── Identidade externa (sujeito OIDC do Keycloak)
  ├── Catálogo
  │   ├── Categoria (UUID, nome único sem diferenciar caixa, descrição opcional, ativa/inativa)
  │   └── Produto (UUID, SKU único normalizado, nome, descrição, categoria obrigatória,
  │       preço decimal, ativo/inativo)
  ├── Estoque (futuro; saldo nunca negativo)
  ├── Vendas (futuro; confirmação depende de estoque suficiente)
  └── Financeiro (futuro; contas a pagar/receber com baixa manual)
```

Identidade externa não equivale a cadastro de usuário de negócio. A API usa `sub` como identificador do principal OIDC e roles do realm para autorização. Não se presume cadastro local de usuário nesta fundação.

Categorias e produtos usam UUIDs, timestamps UTC e desativação lógica para preservar histórico. Produto referencia categoria por FK; novas associações exigem categoria ativa. Produto existente permanece consultável se a categoria for desativada. Estoque não pertence ao agregado Produto nesta etapa.

Limites de agregados, ownership, auditoria, concorrência de estoque e transições de estado dos demais módulos devem ser decididos antes de sua implementação. Multi-tenancy, emissão fiscal e integrações bancárias automáticas estão fora do escopo do MVP.
