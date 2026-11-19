# C-01 — Especificação do módulo de clientes

**Status:** proposta para revisão; nenhuma implementação foi iniciada.

## 1. Objetivo e escopo

Especificar o cadastro mestre de clientes para uso futuro em vendas, seguindo o monólito modular Java/Spring Boot, a API REST `/api/v1`, PostgreSQL/Flyway, Angular standalone e autenticação OIDC já adotados. O MVP atende uma única empresa; não há `tenant_id` nem entidade de organização, conforme ADR-0001.

Inclui cadastro de pessoa física e jurídica, consulta, pesquisa paginada, atualização completa e desativação lógica. Não inclui pedidos, emissão fiscal, consulta externa de CPF/CNPJ, integração com Receita Federal, cobrança, importação em massa ou armazenamento de documentos anexos.

A especificação não cria endpoint ou permissão em funcionamento. As propostas abaixo precisam ser aprovadas antes da implementação.

## 2. Padrões existentes observados

- Backend organizado por módulo em `com.erp.core.<modulo>`, com camadas `api`, `application` e `domain`; entidades JPA, repositories Spring Data, services transacionais e controllers REST.
- Catálogo expõe `/api/v1/categories` e `/api/v1/products`. Requests e responses são records separados; criação retorna `201` e `Location`, PUT completo retorna `200`, consulta/listagem `200` e DELETE lógico `204`.
- IDs são UUID. Timestamps são `Instant` e serializados ISO-8601. Listagens retornam `PageResponse<T>` com `content`, `page`, `size`, `totalElements`, `totalPages`, `first` e `last`.
- Paginação é zero-based; padrão de página 20 e máximo global 100. Ordenação usa `Pageable` do Spring. O catálogo usa `name` crescente por padrão.
- Validação de corpo usa Jakarta Bean Validation. `ApiExceptionHandler` responde com Problem Details: `400` para corpo/parâmetro inválido, `404` recurso ausente e `409` conflito ou restrição relacional; erros de campos podem incluir `errors`.
- Catálogo normaliza valores no service, mantém restrições no PostgreSQL e usa `@PreAuthorize`. ADMIN/MANAGER escrevem; ADMIN/MANAGER/SALES/STOCK_OPERATOR leem. FINANCE não acessa o catálogo.
- Desativação de categoria/produto preserva a linha e não existe reativação. Listagens atuais incluem ativos e inativos salvo filtro explícito.
- As migrations são SQL Flyway versionado e append-only em `apps/api/src/main/resources/db/migration`; o schema de negócio é `erp`. Entidades existentes armazenam `active`, `created_at` e `updated_at` (`TIMESTAMPTZ`).
- Frontend usa Angular standalone, rotas protegidas por `authGuard`, guards por authorities obtidas da API `/api/v1/platform/me`, serviços `HttpClient` por feature e testes com Angular Testing/Vitest. Guards controlam a navegação e visibilidade; autorização efetiva é sempre da API.
- Segurança é stateless, OIDC Keycloak com access token JWT, audiência `erp-core-api`, roles do realm convertidas a `ROLE_*`, deny-by-default e CORS restrito. Apenas health é público.
- OpenAPI existente documenta categorias e produtos e usa bearer JWT global, schemas reutilizados, PageResponse e Problem Details. Clientes ainda não têm contrato nem código no backend; `sales` e `finance` ainda são apenas pacotes de módulo sem domínio implementado.

## 3. Requisitos funcionais propostos

1. Cadastrar PF e PJ em um único recurso, distinguindo-as por `personType`.
2. PF informa nome e CPF; PJ informa razão social e CNPJ. Nome fantasia é opcional para PJ.
3. Permitir inscrição estadual opcional para PJ, inclusive indicação textual de isenção se o negócio aprovar. Não aplicável a PF.
4. Permitir e-mail e telefone opcionais.
5. Permitir endereço de cobrança/contato opcional estruturado.
6. Consultar cliente por UUID e listar com paginação, pesquisa e filtro de situação/tipo.
7. Atualizar dados por PUT completo; campos ausentes opcionais devem ser enviados como `null` conforme contrato. ID, situação e timestamps não são editáveis por esse DTO.
8. Desativar logicamente, sem apagar dados nem alterar referências históricas.
9. Não oferecer cadastro separado de contatos ou múltiplos endereços na primeira versão.

### Dados opcionais avaliados

| Campo | Proposta inicial | Justificativa / decisão pendente |
|---|---|---|
| CEP | Incluir, opcional, texto | Útil para faturamento/entrega; não validá-lo como número. Definir se será normalizado para dígitos ou aceitará hífen. |
| Logradouro | Incluir, opcional | Endereço incompleto é permitido para clientes prospectivos. |
| Número | Incluir, opcional, texto | Texto aceita `S/N`, blocos e formatos não numéricos. |
| Complemento | Incluir, opcional | Campo livre curto. |
| Bairro | Incluir, opcional | Usado em entrega e identificação. |
| Cidade | Incluir, opcional | Informação operacional e futura venda. |
| Estado/UF | Incluir, opcional | Texto; validar UF brasileira somente se a primeira versão for exclusivamente nacional. |
| País | Incluir, opcional | Definir código ISO 3166-1 alpha-2 ou nome; recomenda-se código alpha-2, sem default implícito até decisão. |
| Observações internas | Adiar da primeira versão, salvo aprovação | Podem conter dados pessoais/sensíveis; exigem definição de visibilidade, retenção, auditoria e limite de tamanho. |

**Decisão funcional sugerida:** manter endereço como bloco opcional no cadastro, sem exigir que todos os seus campos sejam preenchidos. O caso de uso de vendas deverá definir endereço de entrega e eventual snapshot histórico separadamente.

## 4. Regras de negócio propostas

### 4.1 Tipo de pessoa e campos

- `personType` obrigatório, enum estável `INDIVIDUAL` ou `COMPANY`.
- `name` obrigatório, após trim e não vazio, até 160 caracteres. Para PF é o nome civil; para PJ é a razão social.
- `tradeName` opcional e até 160 caracteres; recomendado somente para PJ. Rejeitar valor não branco para PF ou ignorar? Recomenda-se rejeitar como erro de validação para evitar dado contraditório.
- `document` obrigatório; CPF para PF e CNPJ para PJ. Armazenar e transportar como string canônica composta apenas de dígitos, respectivamente 11 e 14 posições. Nunca usar tipo numérico, pois documentos podem começar com zero.
- `stateRegistration` opcional e somente aplicável a PJ. A regra para contribuinte isento (texto sentinela, boolean ou ausência) requer decisão; não introduzir um formato sem aprovação.
- `email`, `phone` e endereço são opcionais; quando fornecidos, devem ser não brancos e ter formato/tamanho razoável validado no servidor. Não exigir e-mail ou telefone como identidade do cliente.
- Documento de PF/PJ brasileiro é a única modalidade especificada. Cliente estrangeiro sem CPF/CNPJ está fora do escopo inicial e exige decisão antes de modelar exceções.

### 4.2 Validação e normalização de CPF/CNPJ

- Na entrada, aceitar somente string com dígitos ou máscara brasileira conhecida (CPF `000.000.000-00`; CNPJ `00.000.000/0000-00`). Remover apenas pontuação prevista e espaços externos; rejeitar letras e caracteres inesperados. Persistência e respostas usam dígitos sem máscara.
- Validar comprimento e dígitos verificadores, rejeitando sequências repetidas inválidas. Implementar algoritmo local puro; não fazer consulta externa.
- A regra de CNPJ alfanumérico futuro não integra esta proposta; a adoção deve ser revisada quando o escopo nacional/legislação aplicável exigir.
- A validação deve ocorrer no backend independentemente do frontend. Respostas não devem depender de um texto de erro específico para determinar a lógica.

### 4.3 Unicidade e duplicidade

- Proposta: `document` globalmente único após normalização, independentemente do tipo de pessoa e da situação ativa.
- Inclusão/alteração duplicada retorna `409 Conflict` em Problem Details, sem merge silencioso e sem reativação automática.
- Mudança do próprio documento para o mesmo valor normalizado é permitida. Mudança para documento de outro registro, mesmo inativo, retorna 409.
- Restrição única no PostgreSQL é necessária além da consulta preventiva no service para cobrir concorrência.
- **Aprovação necessária:** confirmar unicidade global incluindo inativos e política de reuso de documento após encerramento/erro cadastral. Recomendação: não permitir reuso; corrigir o cadastro existente por fluxo autorizado/auditado.

### 4.4 Atualização, situação e vendas futuras

- PUT substitui os campos editáveis integralmente; opcionais omitidos/null limpam o valor. Para evitar ambiguidade, o contrato deverá exigir todos os campos editáveis, permitindo `null` só nos opcionais.
- Atualização não altera `active`, `id`, `createdAt` nem `updatedAt`; o timestamp de atualização é mantido pelo lifecycle JPA, conforme entidades do catálogo.
- DELETE significa desativar, é idempotente para registro existente e retorna 204; UUID inexistente retorna 404, como no catálogo.
- Não propor reativação no endpoint de atualização. Caso reativação seja necessária, criar operação explícita e autorizada em decisão posterior, sem inferi-la de PUT.
- Cliente inativo continua consultável para histórico, mas deve ser inelegível para criar/confirmar novas vendas. Momento da checagem (criação, confirmação ou ambos), tratamento de rascunhos já existentes e exceção para ajustes históricos devem ser definidos no SDD de vendas.
- Futuras vendas devem manter FK para o cliente e não podem ser apagadas ao desativar. Definir se o pedido guarda snapshot de razão/nome, documento e endereço no momento da venda para preservar evidência histórica.

### 4.5 Dados pessoais

CPF/CNPJ, contato e endereço são dados pessoais/comerciais. Restringir acesso conforme finalidade, evitar colocar documento completo em logs e erros, não expor campos além do necessário e revisar auditoria/retention. Requisitos LGPD, mascaramento em tela, exportação e anonimização exigem avaliação de negócio/jurídica antes de produção.

## 5. Modelo de dados proposto

Tabela proposta: `erp.customers`, migration futura `V4__customers.sql` somente se a numeração ainda estiver disponível na implementação no momento do trabalho. Confirmar sequência Flyway antes de criar migration; a numeração abaixo é indicativa, não arquivo criado.

| Coluna | Tipo proposto | Regra |
|---|---|---|
| `id` | `UUID` | PK; UUID gerado pela aplicação no padrão de entidades atuais. |
| `person_type` | `VARCHAR(16)` | NOT NULL; `INDIVIDUAL` ou `COMPANY`; check constraint. |
| `name` | `VARCHAR(160)` | NOT NULL, trim, não vazio. Nome civil ou razão social conforme tipo. |
| `trade_name` | `VARCHAR(160)` | NULL; aplicável somente a PJ. |
| `document` | `VARCHAR(14)` | NOT NULL, dígitos canônicos; unique global. Evitar `CHAR`, que pode introduzir padding. |
| `state_registration` | `VARCHAR(32)` | NULL; aplicável a PJ, sujeito à política de isenção/UF. |
| `email` | `VARCHAR(254)` | NULL; tamanho compatível com endereços usuais e validação de formato básica. |
| `phone` | `VARCHAR(32)` | NULL; texto para preservar `+`, DDI e formatação de apresentação. |
| `postal_code` | `VARCHAR(16)` | NULL; política de normalização pendente. |
| `street` | `VARCHAR(160)` | NULL. |
| `number` | `VARCHAR(32)` | NULL; texto. |
| `complement` | `VARCHAR(120)` | NULL. |
| `neighborhood` | `VARCHAR(120)` | NULL. |
| `city` | `VARCHAR(120)` | NULL. |
| `state` | `VARCHAR(64)` | NULL; UF ou subdivisão, conforme escopo geográfico aprovado. |
| `country_code` | `CHAR(2)` ou `VARCHAR(2)` | NULL; ISO alpha-2 somente se esse padrão for aprovado. |
| `active` | `BOOLEAN` | NOT NULL DEFAULT TRUE. |
| `created_at` | `TIMESTAMPTZ` | NOT NULL; persistido em UTC, imutável. |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL; atualizado em alteração. |

Não incluir `tenant_id`: MVP de empresa única (ADR-0001). Não criar tabela genérica de endereços, contatos ou observações sem requisito de múltiplos valores. Para pedidos, relacionamento futuro `sales_orders.customer_id UUID NOT NULL REFERENCES erp.customers(id)`; política de delete restrita e sem exclusão física. FK/snapshot devem ser especificados junto ao módulo de vendas, sem acoplamento interno prematuro entre `sales` e internals de `customers`.

### Constraints e índices propostos

- PK em `id`; CHECK do enum e do comprimento/formato do documento segundo `person_type`; CHECK de `name = btrim(name)` e comprimento > 0; CHECK de campos exclusivamente PJ quando aplicável.
- Índice único `uq_customers_document` em `document` canônico. Caso máscaras sejam persistidas (não recomendado), unique funcional na forma normalizada seria necessário.
- Índice para listagem por situação e nome, sugestão `(active, lower(name))`; confirmar plano de consulta real e comportamento de ordenação/collation.
- O índice unique de documento atende busca exata. Evitar índice em cada campo opcional sem evidência de consulta; filtros por tipo/situação podem exigir índices adicionais se volume justificar.
- Índices, tamanho de campos e restrições devem ser validados em PostgreSQL/Flyway com testes de integração.

## 6. Contrato REST proposto

Base `/api/v1`; recurso `/customers`. O caminho é compatível com o catálogo. Não reutilizar DTOs de categoria/produto.

| Operação | Endpoint | Sucesso proposto |
|---|---|---|
| Listar | `GET /api/v1/customers` | `200 PageResponse<CustomerResponse>` |
| Consultar | `GET /api/v1/customers/{id}` | `200 CustomerResponse` |
| Cadastrar | `POST /api/v1/customers` | `201 CustomerResponse`, header `Location: /api/v1/customers/{id}` |
| Atualizar completo | `PUT /api/v1/customers/{id}` | `200 CustomerResponse` |
| Desativar | `DELETE /api/v1/customers/{id}` | `204`, sem corpo |

Não propor PATCH, endpoint de reativação, exclusão física, bulk ou sub-recursos nesta versão.

### 6.1 Filtros, ordenação e paginação

Proposta para `GET /customers`:

- `name`: contém, case-insensitive; aplicar trim e escape de `%`, `_` e `\\` como no service de produtos.
- `document`: busca exata após normalização para dígitos; evitar busca parcial por documento como padrão por privacidade e seletividade.
- `personType`: enum exato opcional.
- `active`: boolean opcional; omitido inclui ativos e inativos, conforme catálogo.
- `page`, `size`, `sort`: comportamento Spring Pageable, zero-based, tamanho padrão 20, máximo 100. Ordenação padrão sugerida `name,asc`.
- Allowlist de ordenação recomendada: `name`, `document`, `personType`, `active`, `createdAt`, `updatedAt`; rejeitar propriedade desconhecida como 400. Hoje o catálogo entrega `Pageable` diretamente, e o limite global é 100; allowlist de cliente é uma recomendação adicional a confirmar.
- `sort` usa formato Spring `campo,asc|desc` e pode ser repetido. Filtro alterado no frontend deve voltar à página zero.

Busca por documento deve ser restrita a perfis que tenham finalidade de consultar dados cadastrais. Avaliar mascaramento do documento em listagem (p.ex. últimos dígitos) e exposição integral apenas no detalhe, em vez de copiar automaticamente o comportamento simples do catálogo.

### 6.2 DTOs propostos

`CustomerRequest` para POST e PUT (DTOs separados são preferíveis se criação e atualização evoluírem de forma diferente):

```json
{
  "personType": "COMPANY",
  "name": "Empresa Exemplo Ltda.",
  "tradeName": "Exemplo",
  "document": "00123456000199",
  "stateRegistration": null,
  "email": "contato@example.com",
  "phone": "+55 65 3333-4444",
  "address": {
    "postalCode": "78000000",
    "street": "Rua Central",
    "number": "100",
    "complement": null,
    "neighborhood": "Centro",
    "city": "Cuiabá",
    "state": "MT",
    "countryCode": "BR"
  }
}
```

O exemplo é estrutural, não afirma que CPF/CNPJ literal é válido; payload real precisa passar algoritmo verificador. Decidir se endereço será objeto aninhado ou colunas expostas no DTO. Recomenda-se objeto `address` para semântica clara, persistido inicialmente como colunas simples na mesma tabela.

`CustomerResponse` acrescenta `id`, `active`, `createdAt`, `updatedAt` aos campos de negócio. `document` retorna string canônica somente dígitos; apresentação mascarada pertence à UI. `address` pode ser `null` quando todos os campos estiverem ausentes, conforme decisão de contrato. Datas são ISO-8601 com UTC (`Instant`); IDs são UUID string padrão. Requests de atualização não incluem ID/situação/timestamps.

### 6.3 Códigos e erros

- `400 application/problem+json`: JSON inválido, UUID/tipo/data inválidos, campo obrigatório, tamanho/formato inválido ou dígito verificador incorreto. Para validação Bean Validation, manter `errors` por campo conforme `ApiExceptionHandler`.
- `401`: access token ausente/inválido/expirado.
- `403`: autenticado sem authority para operação.
- `404`: UUID inexistente em consulta, PUT ou DELETE.
- `409`: documento já cadastrado, inclusive em registro inativo, ou conflito de estado/regra de domínio.
- `201`, `200`, `204` conforme a tabela de operações.
- Não devolver CPF/CNPJ completo no `detail` de erro. Mapear conflito para campo `document` se a forma de Problem Details for ampliada; não depender do texto interno de exceção.

## 7. Segurança proposta

O backend continuará usando resource server OIDC/JWT, validação existente de issuer/audience/assinatura/expiração, authorities `ROLE_*`, CORS e deny-by-default. Não ampliar permissões globais nem tratar guard Angular como barreira de autorização. Aplicar método a método com `@PreAuthorize`, acompanhando a matriz aprovada.

A política existente documenta a matriz geral como baseline, mas chama as permissões além do catálogo de pendentes. Matriz abaixo é proposta específica e requer aprovação de negócio:

| Operação de clientes | ADMIN | MANAGER | SALES | STOCK_OPERATOR | FINANCE |
|---|---:|---:|---:|---:|---:|
| Listar/consultar | Sim | Sim | Sim | Não | Sim* |
| Criar | Sim | Sim | Sim* | Não | Não |
| Editar | Sim | Sim | Sim* | Não | Não |
| Desativar | Sim | Sim | Não* | Não | Não |

`*` Decisão pendente. Recomendação: SALES consulta/cria/edita porque prepara vendas; permitir desativação apenas a ADMIN/MANAGER para evitar remoção operacional acidental. FINANCE consulta o mínimo necessário para faturamento/contas a receber, preferencialmente com documento minimizado; sem escrita. STOCK_OPERATOR não precisa de cadastro de clientes. ADMIN/MANAGER têm administração completa. A permissão de criar/editar por SALES e a extensão da leitura de FINANCE precisam ser aprovadas antes da configuração.

Na SPA, criar rota e guard específico de clientes após aprovação da matriz. A UI deve ocultar ações sem authority, tratar 401/403 e ainda deixar o backend impor todas as regras. A chamada `/me` permanece fonte de authorities de interface.

## 8. Arquitetura de implementação futura

- Módulo `com.erp.core.customers` com `api`, `application`, `domain`; não adicionar classes internas ao `catalog`.
- API: `CustomerController`, records request/response e mapper; anotação Bean Validation customizada ou validação de domínio testável para documento e regras condicionais.
- Aplicação: `CustomerService` transacional com normalização, consulta de duplicidade e regras de ativo.
- Domínio/persistência: `Customer`, `CustomerRepository` e consultas paginadas via Specification, sem acoplar a internals de `sales`.
- Erros: handler atual hoje importa exceções específicas de catálogo. Para reuso correto, extrair tipos de erro comuns para `platform` ou adicionar tipos de domínio do módulo e mapear explicitamente no `ApiExceptionHandler`. Evitar dependência arquitetural `platform -> catalog` crescer para `platform -> todos os módulos`; registrar como evolução necessária antes da implementação.
- OpenAPI: adicionar paths e schemas ao `docs/api-contract.yaml`, parâmetros de busca, Problem Details e segurança. Documentar dados sensíveis e campos retornados/mascarados.
- Frontend futuro: feature standalone `customers/`, modelos próprios, `CustomerApiService`, listagem/filtros, formulário tipado e mensagens de validação. Não alterar módulos de categoria/produto nem os modelos de catálogo.
- Persistência via migration Flyway append-only, após verificar versão mais alta real e validar em PostgreSQL. Atualizar documentação arquitetural/domain model somente junto à implementação aprovada.

### Impactos arquiteturais

1. O handler global acopla `platform.error` a `CatalogExceptions`; a expansão de conflitos/not-found para clientes justifica abstrair erros comuns ou registrar handlers sem dependência circular.
2. Dados pessoais demandam autorização e minimização distintas do catálogo. FINANCE e SALES não devem receber automaticamente o mesmo escopo de leitura do catálogo.
3. A futura relação com vendas precisa preservar histórico mesmo após desativação e precisa decidir snapshot de dados do cliente no pedido.
4. A normalização de documento deve ser idêntica entre aplicação e índice único; restrição PostgreSQL é a última barreira para concorrência.
5. Endereço embutido é simples para MVP; múltiplos endereços/contatos no futuro exigem migração de modelo e contrato, portanto não prometer essa cardinalidade agora.
6. Não há necessidade de novo ADR neste momento: monólito modular, PostgreSQL/Flyway, OIDC e versionamento `/api/v1` já estão aceitos. Um ADR só se justifica se uma decisão transversal nova surgir, como política de auditoria/retention de dados pessoais ou arquitetura compartilhada de erros.

## 9. Validações propostas

| Campo/regra | Validação proposta |
|---|---|
| `personType` | Obrigatório, enum conhecido. |
| `name` | Obrigatório, trim, 1–160 caracteres. |
| `tradeName` | Opcional, máximo 160; somente PJ. |
| `document` | Obrigatório; CPF 11 ou CNPJ 14 dígitos canônicos, dígitos verificadores válidos; string, preservar zeros iniciais. |
| `stateRegistration` | Opcional, máximo 32; somente PJ; regras estaduais/isento a decidir. |
| `email` | Opcional, trim, máximo 254, validação sintática prática sem prometer existência/entregabilidade. |
| `phone` | Opcional, máximo 32; preservar `+`; normalização e extensão a decidir. |
| `postalCode` | Opcional, máximo 16; CEP brasileiro ou código internacional conforme decisão. |
| Campos de endereço | Opcionais; limites conforme modelo; sem exigir preenchimento parcial/coerente até definição. |
| `active`, `id`, timestamps | Não aceitos em DTO de escrita. |
| Campos desconhecidos | Definir política Jackson: não aceitar ou ignorar; seguir configuração real do projeto, sem afirmar comportamento não confirmado. |

Validação de CPF/CNPJ deve ter testes para válidos, inválidos, comprimento errado, pontuação aceita, entrada malformada, zero inicial e casos limite de sequências repetidas. Duplicidade concorrente deve ser garantida por índice unique e convertida em 409 sem revelar dados de outro cliente.

## 10. Testes previstos

### Backend

- Unitários de normalização/validação CPF e CNPJ, regras condicionais PF/PJ, campos opcionais, nome trimado e documento com zeros iniciais.
- Controller HTTP real: rotas, JSON, UUID, `201`/Location, `200`, `204`, `400` com `errors`, `404`, `409`, `401` e `403` por role.
- Service: criação, PUT completo, ausência de mudança de situação em PUT, duplicidade consigo mesmo vs. outro registro, duplicidade com inativo e desativação idempotente.
- Repository/PostgreSQL: constraints, índice único, normalização, busca e paginação/ordenação, concorrência de documentos duplicados.
- Migration Flyway executada em PostgreSQL; nenhuma exclusão/alteração destrutiva em migrations anteriores.
- Segurança: matriz integral por método e role, incluindo perfil desconhecido e ausência de token.
- OpenAPI: paths, métodos, schemas, enum, formatos, filtros, respostas e bearer auth.
- Se regras de venda forem implementadas depois: rejeição de cliente inativo no momento de uso decidido; associação/histórico continua disponível.

### Angular (fase futura)

- Serviço HTTP: paths, filtros, paginação/ordenação, request PUT completo e códigos HTTP.
- Formulário: PF/PJ alternando campos/validadores, CPF/CNPJ com máscara visual sem persistir máscara, valores opcionais, edição e erros 400/409.
- Lista: busca por nome/documento, filtros ativo/inativo/tipo, vazio/loading/erro, paginação e minimização de documento segundo política.
- Guards e ações visuais para a matriz aprovada, mantendo testes da API como autoridade final.
- Não registrar documento completo em console, telemetria ou snapshots de teste desnecessários.

## 11. Critérios de aceite da especificação

1. Escopo diferencia PF/PJ e lista dados obrigatórios/opcionais sem criar endpoints ou campos como implementação existente.
2. CPF/CNPJ têm normalização canônica, verificação de dígitos, unicidade e comportamento de duplicidade explícitos.
3. PUT completo, desativação lógica e efeito em vendas futuras têm regras e lacunas documentadas.
4. Modelo proposto define tipos, constraints, índices e compatibilidade com UUID, timestamps, schema `erp` e Flyway append-only.
5. API proposta define operações, paginação, filtros, DTOs, respostas, erros, datas, identificadores e documentos.
6. A matriz de segurança é apresentada como proposta e não altera a matriz atual.
7. Dados pessoais, auditoria e minimização estão identificados como preocupação e decisão de negócio/jurídica.
8. Testes de backend, integração PostgreSQL, segurança, OpenAPI e Angular futuro estão especificados.
9. Nenhum código, migration, dependência ou contrato existente é alterado nesta etapa.

## 12. Decisões pendentes antes de C-02

1. Confirmar nomenclatura do campo `name` para PF/razão social e se `tradeName` só se aplica a PJ.
2. Aprovar cadastro nacional apenas com CPF/CNPJ ou prever pessoa estrangeira/documento alternativo.
3. Aprovar unicidade global incluindo clientes inativos e proibição de reutilização do documento.
4. Definir tratamento de inscrição estadual isenta, produtor rural e regras por UF.
5. Definir quais campos de endereço são necessários no MVP, regra de CEP e se `countryCode` é ISO alpha-2.
6. Decidir se observações internas entram no MVP e quais perfis podem lê-las.
7. Aprovar matriz de acesso específica de clientes, sobretudo SALES para escrita/desativação e FINANCE para leitura, e se documento será mascarado.
8. Definir se existe reativação e, se sim, fluxo/roles/auditoria; proposta atual não contempla.
9. Para vendas, definir quando cliente inativo é bloqueado e quais dados são congelados no pedido.
10. Definir política de auditoria, retenção, correção/exclusão e atendimento LGPD para documentos e contatos.
11. Decidir DTO de endereço aninhado vs. campos planos e o tratamento de endereço totalmente vazio (`null` vs. objeto vazio).
12. Confirmar allowlist de ordenação e tamanho/política de busca por documento.
13. Definir caso de uso de auditoria de mudanças: regra baseline pede autoria/momento para mudanças relevantes, mas o catálogo atualmente apenas mantém timestamps; determinar se clientes exigirão audit trail desde o primeiro release.

## Referências internas analisadas

- `docs/decisions/0001-modular-monolith.md`, `0002-postgresql-flyway.md`, `0003-oidc-keycloak.md`
- `docs/security-model.md`, `docs/domain-model.md`, `docs/business-rules.md`, `docs/requirements.md`
- `docs/api-contract.yaml`
- `docs/specs/catalog-angular.md`, `docs/specs/products-angular.md`
- `apps/api/src/main/java/com/erp/core/catalog/{api,application,domain}`
- `apps/api/src/main/java/com/erp/core/platform/{error,security,web}`
- `apps/api/src/main/resources/db/migration/V1__foundation_schema.sql` até `V3__catalog_nonblank_constraints.sql`
- `apps/web/src/app/app.routes.ts`, `apps/web/src/app/catalog/`, `apps/web/src/app/auth/`
