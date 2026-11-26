# Convenções de banco de dados

## Plataforma e schemas

- PostgreSQL 17.11 é a versão fixada no Compose local. Dados de negócio ficam no schema `erp`; Keycloak usa database separado `erp_keycloak` no serviço local.
- Migrations Flyway SQL ficam em `apps/api/src/main/resources/db/migration`. As versões presentes nesta revisão vão de V1 a V5. Confirme o maior número existente antes de propor uma nova migration.
- Hibernate está configurado com `ddl-auto: validate`; schema é alterado por migration, não por `ddl-auto: update`.

## Nomes, chaves e integridade

- Use nomes SQL `snake_case` e tabelas por módulo no schema `erp`, conforme as migrations existentes. Evite identificadores reservados e `CHAR` quando padding puder alterar comparação/mapeamento.
- Entidades de negócio usam UUID gerado pela aplicação. Chaves estrangeiras devem refletir relações reais e constraints devem preservar integridade referencial.
- Declare obrigatoriedade, limites, valores enumerados, unicidade e relações no banco quando fizer sentido, além das validações da aplicação. Índices devem atender filtros/ordenações observados; evite índices indiscriminados.
- Não inclua normalização documental ou regra que dependa de fonte normativa em SQL. A canonicalização de documentos deve ocorrer antes da persistência e a unicidade deve incidir sobre o valor canônico.

## Tipos de dados

- Dinheiro não usa `float`, `double` ou `NUMERIC` sem escala/precisão definida. O produto persistido usa `NUMERIC(14,2)` e `BigDecimal`; o contrato v2 transporta decimal canônico como string. Consulte [contrato monetário](specs/monetary-contract.md).
- Datas auditáveis usam `TIMESTAMPTZ` no PostgreSQL e `Instant` no Java. Não usar horário local sem decisão explícita.
- CPF/CNPJ e outros identificadores que preservam zeros/letras são strings; clientes têm `document VARCHAR(14)`. Não converter para número.
- Campos opcionais permanecem nullable quando a especificação assim determina. O endereço de clientes é embutido como colunas, sem tabela separada, conforme a especificação de clientes.

## Auditoria e situação

- Entidades atuais do catálogo e clientes usam `active`, `created_at` e `updated_at`; clientes mantêm esses dois timestamps sem trilha completa de auditoria na tabela. Não extrapole esse padrão para auditoria completa.
- Desativação lógica é apropriada quando histórico/relacionamentos precisam permanecer. Evite exclusão física que quebre referências ou histórico.

## Evolução segura de schema

- Migrations compartilhadas são append-only: nunca editar migration que já foi aplicada em ambiente compartilhado. Mudança posterior exige nova migration versionada.
- Para alteração compatível com deploys simultâneos, avalie expand/contract e preservação dos dados; declare impactos e estratégia de rollback quando aplicável.
- Valide compatibilidade entre tipo SQL, nulabilidade, tamanho, enum e mapeamento JPA com Hibernate `validate` e teste em PostgreSQL real. Não assumir compatibilidade apenas por compilação.
- Evite consultas não paginadas para coleções potencialmente grandes. Filtros/ordenação devem ser parametrizados e limitados por allowlist quando aceitam propriedades externas.
- Separe banco de desenvolvimento `erp_core` e banco de teste `erp_core_test`. A configuração de teste deve receber URL/credenciais explícitas e nunca apontar para produção ou dados que devam ser preservados.
