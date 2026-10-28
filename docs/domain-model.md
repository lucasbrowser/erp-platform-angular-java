# Modelo de domínio inicial

O modelo desta fase é deliberadamente conceitual e não gera tabelas de negócio.

```text
Organização (conceito, multiempresa pendente)
  ├── Usuário / identidade externa (Keycloak)
  ├── Módulos de negócio (a descobrir)
  └── Registros auditáveis (a especificar)

Aplicação modular (ERP Core)
  ├── Identidade e acesso (infraestrutura)
  ├── Catálogo (futuro)
  ├── Estoque (futuro)
  ├── Vendas (futuro)
  └── Financeiro (futuro)
```

Identidade e organização são conceitos diferentes: o identificador do sujeito OIDC não deve ser tratado como cadastro de usuário de negócio. Limites de agregados, ownership dos dados, cardinalidades e tenancy serão definidos após requisitos funcionais aprovados.
