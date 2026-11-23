# C-01 — Especificação do módulo de clientes

**Status:** regras funcionais e decisões técnicas consolidadas para revisão; nenhuma implementação foi iniciada.

## 1. Objetivo e escopo

Especificar o cadastro mestre de clientes para uso futuro em vendas, seguindo o monólito modular Java/Spring Boot, a API REST `/api/v1`, PostgreSQL/Flyway, Angular standalone e autenticação OIDC já adotados. O MVP atende uma única empresa; não há `tenant_id` nem entidade de organização, conforme ADR-0001.

Inclui cadastro de pessoa física e jurídica, consulta, pesquisa paginada, atualização completa e desativação lógica. Não inclui pedidos, emissão fiscal, consulta externa de CPF/CNPJ, integração com Receita Federal, cobrança, importação em massa ou armazenamento de documentos anexos.

A especificação não cria endpoint ou permissão em funcionamento. As regras funcionais e técnicas consolidadas abaixo foram aprovadas para orientar a implementação. Permanecem explicitamente pendentes apenas os pontos indicados na seção 12, incluindo os que dependem do módulo `sales` ou de avaliação jurídica.

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
3. Permitir inscrição estadual opcional para PJ. Não aplicável a PF; não há regra especial de isenção ou produtor rural no MVP.
4. Permitir e-mail e telefone opcionais.
5. Permitir endereço de cobrança/contato opcional estruturado.
6. Consultar cliente por UUID e listar com paginação, pesquisa e filtro de situação/tipo.
7. Atualizar dados por PUT completo; campos ausentes opcionais devem ser enviados como `null` conforme contrato. ID, situação e timestamps não são editáveis por esse DTO.
8. Desativar logicamente, sem apagar dados nem alterar referências históricas.
9. Não oferecer cadastro separado de contatos ou múltiplos endereços na primeira versão.

### Dados opcionais avaliados

| Campo | Decisão para o MVP | Observação |
|---|---|---|
| CEP | Incluir, opcional, texto | CEP brasileiro é normalizado para oito dígitos, sem máscara; não validá-lo como número. |
| Logradouro | Incluir, opcional | Endereço incompleto é permitido para clientes prospectivos. |
| Número | Incluir, opcional, texto | Texto aceita `S/N`, blocos e formatos não numéricos. |
| Complemento | Incluir, opcional | Campo livre curto. |
| Bairro | Incluir, opcional | Usado em entrega e identificação. |
| Cidade | Incluir, opcional | Informação operacional e futura venda. |
| Estado/UF | Incluir, opcional | Texto; validação estadual complexa fica fora do MVP. |
| País | Incluir no objeto de endereço como `countryCode` opcional na entrada | Usar ISO 3166-1 alpha-2; aplicar `BR` quando um endereço informado não trouxer país. Isso não habilita clientes estrangeiros. |
| Observações internas | Fora do MVP | Não haverá campo de observações internas nesta versão. |

**Regra consolidada:** a API recebe e responde um objeto opcional `address`, com `postalCode`, `street`, `number`, `complement`, `neighborhood`, `city`, `state` e `countryCode`. Todos os campos são opcionais. Os valores são persistidos como colunas de `erp.customers`, sem tabela separada de endereços. Se todos os campos de endereço estiverem ausentes, a resposta traz `address: null`. Quando há endereço informado e `countryCode` não é enviado, usar `BR`. CEP é string; para CEP brasileiro, remover a máscara e persistir oito dígitos. Não consultar serviços externos de CEP nem validar endereço externamente. Regras de endereço de entrega e snapshot histórico continuam no SDD de `sales`.

## 4. Regras de negócio consolidadas

### 4.1 Tipo de pessoa e campos

- `personType` obrigatório, enum estável `INDIVIDUAL` (PF) ou `COMPANY` (PJ).
- `name` obrigatório, após trim e não vazio, até 160 caracteres. Para PF representa nome civil; para PJ, razão social.
- `tradeName` opcional, até 160 caracteres e exclusivo de `COMPANY`; para `INDIVIDUAL` deve ser ausente ou nulo. O backend rejeitará valor informado para PF.
- `document` obrigatório e sempre string: CPF para `INDIVIDUAL`, CNPJ para `COMPANY`. Preservar zeros iniciais e não converter para tipo numérico.
- `stateRegistration` opcional e exclusiva de `COMPANY`. Não implementar validação estadual complexa, regra especial de isenção ou tratamento especial de produtor rural no MVP.
- E-mail e telefone são opcionais. Endereço também é opcional e não exige preenchimento de todos os seus campos. Observações internas não fazem parte do MVP.
- Clientes estrangeiros estão fora do MVP: não aceitar passaporte nem criar identificador/documento alternativo; CPF ou CNPJ permanece obrigatório.

### 4.2 Validação e normalização de CPF/CNPJ

- CPF possui 11 dígitos numéricos. CNPJ possui 14 posições e deve suportar tanto o formato numérico legado quanto o formato alfanumérico vigente.
- Aceitar os formatos sem máscara e as máscaras oficiais aplicáveis; remover as máscaras na normalização. Persistir e transportar documento como string canônica sem máscara. Para CNPJ alfanumérico, normalizar letras para maiúsculas de forma consistente, preservar letras e números nas posições correspondentes e nunca converter letras em números. A forma canônica final deve seguir a documentação oficial vigente.
- Validar formato e dígitos verificadores no backend, de acordo com a documentação oficial vigente da Receita Federal para CPF e CNPJ, incluindo o CNPJ alfanumérico. Esta especificação não descreve nem inventa algoritmos; a implementação deverá consultar e referenciar a documentação oficial aplicável no momento do desenvolvimento.
- Não consultar a Receita Federal nem qualquer serviço externo. A validação local não confirma situação cadastral ou titularidade.
- A validação deve ocorrer no backend independentemente do frontend. Respostas não devem depender de texto interno de erro para determinar lógica.

### 4.3 Unicidade e duplicidade

- `document` é globalmente único pela representação canônica normalizada, incluindo clientes ativos e inativos. A comparação e a constraint usam exatamente essa forma; não é permitido reutilizar documento.
- Inclusão/alteração duplicada retorna `409 Conflict` em Problem Details, sem merge silencioso nem reativação automática.
- O cliente pode manter o próprio documento ou alterá-lo se nenhum outro registro possuir o valor normalizado. Documento pertencente a outro registro, inclusive inativo, retorna 409.
- O banco garante unicidade por constraint/índice único sobre o valor canônico, além da verificação na aplicação para produzir erro de domínio. Isso também protege contra concorrência.

### 4.4 Atualização, situação e vendas futuras

- PUT substitui os campos editáveis integralmente; opcionais omitidos/null limpam o valor. O contrato deverá exigir todos os campos editáveis, permitindo `null` somente nos opcionais.
- Atualização não altera `active`, `id`, `createdAt` nem `updatedAt`; o timestamp de atualização é mantido pelo lifecycle JPA, conforme entidades do catálogo.
- DELETE significa desativar, é idempotente para registro existente e retorna 204; UUID inexistente retorna 404, como no catálogo.
- Não haverá reativação no MVP. PUT não altera `active`; não é permitido reativar implicitamente por atualização. Se necessária futuramente, reativação será operação explícita e autorizada.
- Cliente inativo permanece consultável para histórico e não pode ser usado em novas vendas. As regras de validação da situação na criação e confirmação de vendas serão especificadas no módulo `sales`.
- O tratamento de dados cadastrais congelados em pedidos (snapshot histórico) será definido no módulo `sales`.

### 4.5 Dados pessoais

CPF/CNPJ, contato e endereço são dados pessoais/comerciais. Restringir acesso conforme a matriz da seção 7, evitar colocar documento completo em logs e erros e aplicar minimização por perfil no backend. No MVP, persistir somente `createdAt` e `updatedAt`: não criar histórico completo de alterações, tabela de auditoria, `createdBy` ou `updatedBy`. Auditoria completa pode ser evolução futura. Retenção e descarte, exportação, anonimização e auditoria completa ficam fora do escopo de implementação do módulo `customers`. Requisitos LGPD devem ser definidos e avaliados antes da entrada em produção.

## 5. Modelo de dados proposto

Tabela proposta: `erp.customers`, migration futura `V4__customers.sql` somente se a numeração ainda estiver disponível na implementação no momento do trabalho. Confirmar sequência Flyway antes de criar migration; a numeração abaixo é indicativa, não arquivo criado.

| Coluna | Tipo proposto | Regra |
|---|---|---|
| `id` | `UUID` | PK; UUID gerado pela aplicação no padrão de entidades atuais. |
| `person_type` | `VARCHAR(16)` | NOT NULL; `INDIVIDUAL` ou `COMPANY`; check constraint. |
| `name` | `VARCHAR(160)` | NOT NULL, trim, não vazio. Nome civil ou razão social conforme tipo. |
| `trade_name` | `VARCHAR(160)` | NULL; aplicável somente a PJ. |
| `document` | `VARCHAR(14)` | NOT NULL; comporta 11 dígitos do CPF ou 14 posições do CNPJ numérico/alfanumérico; persistir string canônica sem máscara e com letras normalizadas para maiúsculas; unique global. Evitar `CHAR`, que pode introduzir padding. |
| `state_registration` | `VARCHAR(32)` | NULL; somente PJ; sem regra especial de isenção/produtor rural ou validação estadual complexa no MVP. |
| `email` | `VARCHAR(254)` | NULL; tamanho compatível com endereços usuais e validação de formato básica. |
| `phone` | `VARCHAR(32)` | NULL; texto para preservar `+`, DDI e formatação de apresentação. |
| `postal_code` | `VARCHAR(16)` | NULL; texto. Quando `country_code` for `BR`, armazenar CEP normalizado para oito dígitos, sem máscara. |
| `street` | `VARCHAR(160)` | NULL. |
| `number` | `VARCHAR(32)` | NULL; texto. |
| `complement` | `VARCHAR(120)` | NULL. |
| `neighborhood` | `VARCHAR(120)` | NULL. |
| `city` | `VARCHAR(120)` | NULL. |
| `state` | `VARCHAR(64)` | NULL; UF ou subdivisão, conforme escopo geográfico aprovado. |
| `country_code` | `CHAR(2)` | NULL quando não houver endereço; quando endereço for informado, armazenar ISO 3166-1 alpha-2, com `BR` como padrão de entrada. |
| `active` | `BOOLEAN` | NOT NULL DEFAULT TRUE. |
| `created_at` | `TIMESTAMPTZ` | NOT NULL; persistido em UTC, imutável; exposto como `createdAt`. |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL; atualizado em alteração; exposto como `updatedAt`. |

Não incluir `tenant_id`: MVP de empresa única (ADR-0001). Não criar tabela separada de endereços nem tabela genérica de contatos ou observações sem requisito de múltiplos valores. As oito propriedades do objeto `address` são armazenadas em colunas da própria `erp.customers`. Se os oito campos estiverem ausentes ou nulos, manter todas as colunas de endereço nulas e responder `address: null`. Se qualquer campo for informado, manter o objeto; quando `countryCode` estiver ausente, aplicar/persistir `BR`. Para CEP com país `BR`, armazenar oito dígitos sem máscara; `postalCode` continua texto. Para pedidos, relacionamento futuro `sales_orders.customer_id UUID NOT NULL REFERENCES erp.customers(id)`; política de delete restrita e sem exclusão física. FK/snapshot devem ser especificados junto ao módulo de vendas, sem acoplamento interno prematuro entre `sales` e internals de `customers`.

### Constraints e índices propostos

- PK em `id`; CHECK do enum, comprimento e formato básico do documento por tipo; CHECK de `name = btrim(name)` e comprimento > 0; CHECK de campos exclusivamente PJ quando aplicável. A validação dos dígitos verificadores segue a documentação oficial vigente e fica na aplicação, sem algoritmo inventado na especificação.
- Índice único `uq_customers_document` sobre `document` canônico, igual à representação normalizada e persistida pela aplicação. Máscaras não são armazenadas; não usar conversão de letras para números.
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

Contrato consolidado para `GET /customers`:

- `name`: busca parcial case-insensitive; aplicar trim e escapar `%`, `_` e `\\` para que sejam tratados como caracteres literais, seguindo o padrão do service de produtos.
- `document`: busca exata após a mesma normalização canônica sem máscara usada na persistência (CPF numérico ou CNPJ alfanumérico em maiúsculas). Não oferecer busca parcial por CPF/CNPJ. FINANCE não pode filtrar por documento; aplicar a regra de perfil descrita na seção 7.
- `personType`: filtro opcional por enum exato `INDIVIDUAL` ou `COMPANY`.
- `active`: filtro opcional booleano. Quando omitido, a lista inclui clientes ativos e inativos.
- `page`: zero-based; padrão 0. `size`: padrão 20, máximo 100.
- `sort`: padrão `name,asc`. Aceitar múltiplos parâmetros `sort` no formato Spring `property,direction`.
- Allowlist de propriedades para ordenação: `name`, `document`, `personType`, `active`, `createdAt`, `updatedAt`. Qualquer propriedade desconhecida deve resultar em `400 Bad Request`, sem repassar propriedade arbitrária ao `Pageable`.
- A listagem retorna o `PageResponse<T>` adotado pelo catálogo. Alterar filtros no Angular reinicia a página para zero.

As projeções de resposta e restrições de busca por perfil estão definidas na seção 7.

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

Todos os campos dentro de `address` são opcionais. O objeto também pode ser omitido ou `null`; se não houver nenhum campo de endereço, a resposta retorna `address: null`. Se `address` for informado com algum campo diferente de `countryCode`, e `countryCode` estiver ausente, o backend assume `BR`. `postalCode` é string; CEP brasileiro é normalizado para oito dígitos sem máscara. Não há consulta externa de CEP nem validação externa do endereço. O exemplo é estrutural, não afirma que CPF/CNPJ literal é válido; payload real precisa passar validação oficial.

`CustomerResponse` acrescenta `id`, `active`, `createdAt`, `updatedAt` aos campos de negócio. `document` é sempre string canônica sem máscara; CNPJ alfanumérico preserva letras, normalizadas para maiúsculas, sem conversão entre letras e números. As projeções por perfil, inclusive a minimização obrigatória de FINANCE, seguem a seção 7. `address` é `null` se todos os seus campos estiverem ausentes. Datas são ISO-8601 com UTC (`Instant`); IDs são UUID string padrão. Requests de atualização não incluem ID/situação/timestamps.

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

Matriz aprovada para o módulo de clientes:

| Operação de clientes | ADMIN | MANAGER | SALES | STOCK_OPERATOR | FINANCE |
|---|---:|---:|---:|---:|---:|
| Listar/consultar | Sim | Sim | Sim | Não | Sim (dados minimizados) |
| Criar | Sim | Sim | Sim | Não | Não |
| Editar | Sim | Sim | Sim | Não | Não |
| Desativar | Sim | Sim | Não | Não | Não |

ADMIN e MANAGER recebem os dados cadastrais completos e possuem acesso administrativo completo. SALES recebe os dados necessários à finalidade comercial aprovada e pode listar, consultar, criar e editar, mas não desativar. FINANCE tem leitura apenas, não pode filtrar por documento e nunca recebe CPF/CNPJ completo, nem na listagem nem no detalhe. Para FINANCE, o backend usa projeção ou DTO específico minimizado; é proibido reutilizar o DTO administrativo integral e apenas ocultar campos no Angular. STOCK_OPERATOR não acessa o módulo. Nenhum perfil FINANCE recebe escrita. Autorização e minimização são aplicadas no backend; Angular controla apenas navegação e visibilidade.

Na SPA, criar rota e guard específico de clientes conforme a matriz consolidada. A UI deve ocultar ações sem authority, tratar 401/403 e ainda deixar o backend impor todas as regras. A chamada `/me` permanece fonte de authorities de interface. FINANCE deve ver somente dados minimizados e não deve receber o documento completo via API para apenas ocultá-lo visualmente.

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
2. Dados pessoais demandam autorização e minimização distintas do catálogo; projeções por perfil seguem a regra consolidada da seção 7.
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
| `document` | Obrigatório como string; CPF com 11 dígitos numéricos ou CNPJ com 14 posições (numérico legado ou alfanumérico vigente); remover máscara, preservar zeros, normalizar letras para maiúsculas sem conversão para números e validar conforme documentação oficial vigente da Receita Federal. |
| `stateRegistration` | Opcional, máximo 32; somente PJ; sem validação estadual complexa nem regra especial de isenção/produtor rural no MVP. |
| `email` | Opcional, trim, máximo 254, validação sintática prática sem prometer existência/entregabilidade. |
| `phone` | Opcional, máximo 32; preservar `+`; normalização e extensão a decidir. |
| `address` | Opcional; objeto com `postalCode`, `street`, `number`, `complement`, `neighborhood`, `city`, `state` e `countryCode`; todos os campos opcionais. Se todos ausentes, resposta `null`. |
| `postalCode` | Texto; quando o país é `BR`, CEP normalizado para oito dígitos, removendo máscara. Para outros códigos de país, não aplicar formatação numérica brasileira. Sem consulta externa de CEP. |
| `countryCode` | Código ISO 3166-1 alpha-2; se endereço informado não trouxer país, padrão `BR`. |
| Campos de endereço | Persistidos como colunas em `erp.customers`; não criar tabela de endereços. Não consultar ou validar endereço externamente. |
| `active`, `id`, `createdAt`, `updatedAt` | Não aceitos em DTO de escrita. Somente `createdAt` e `updatedAt` são mantidos no MVP; sem `createdBy`, `updatedBy` ou histórico de auditoria. |
| Campos desconhecidos | Definir política Jackson: não aceitar ou ignorar; seguir configuração real do projeto, sem afirmar comportamento não confirmado. |

Validação de CPF/CNPJ deve ter testes alinhados à documentação oficial vigente da Receita Federal, cobrindo CPF, CNPJ numérico legado e CNPJ alfanumérico vigente, válidos/inválidos, tamanho, pontuação aceita, entrada malformada e zeros iniciais. Não codificar algoritmo derivado desta especificação. Duplicidade concorrente deve ser garantida por índice unique e convertida em 409 sem revelar dados de outro cliente.

## 10. Testes previstos

### Backend

- Unitários de normalização/validação CPF e CNPJ, regras condicionais PF/PJ, campos opcionais, nome trimado e documento com zeros iniciais.
- Controller HTTP real: rotas, JSON com documento sempre string, CNPJ alfanumérico sem conversão, UUID, `201`/Location, `200`, `204`, `400` com `errors`, `404`, `409`, `401` e `403` por role.
- Service: criação, PUT completo, ausência de mudança de situação em PUT, duplicidade consigo mesmo vs. outro registro, duplicidade com inativo e desativação idempotente.
- Repository/PostgreSQL: constraints, `VARCHAR(14)`, índice único sobre a representação canônica, equivalência de caixa após normalização, busca e paginação/ordenação, concorrência de documentos duplicados.
- Migration Flyway executada em PostgreSQL; nenhuma exclusão/alteração destrutiva em migrations anteriores.
- Segurança: matriz integral por método e role, incluindo perfil desconhecido e ausência de token; verificar que FINANCE usa projeção/DTO próprio sem CPF/CNPJ completo e não consegue filtrar por documento.
- OpenAPI: paths, métodos, schemas, enum, formatos, filtros, respostas e bearer auth.
- No módulo `sales`: testar quando a situação ativa do cliente é verificada nas operações comerciais e como o snapshot histórico mantém os dados definidos por aquele módulo.

### Angular (fase futura)

- Serviço HTTP: paths, filtros, paginação/ordenação, request PUT completo e códigos HTTP.
- Formulário: PF/PJ alternando campos/validadores, CPF/CNPJ com máscara visual sem persistir máscara, valores opcionais, edição e erros 400/409.
- Lista: busca por nome/documento, filtros ativo/inativo/tipo, vazio/loading/erro, paginação e minimização de documento segundo política.
- Guards e ações visuais para a matriz aprovada, mantendo testes da API como autoridade final.
- Não registrar documento completo em console, telemetria ou snapshots de teste desnecessários.

## 11. Critérios de aceite da especificação

1. Escopo diferencia PF/PJ e lista dados obrigatórios/opcionais sem criar endpoints ou campos como implementação existente.
2. CPF/CNPJ são strings; CPF tem 11 dígitos e CNPJ 14 posições em `VARCHAR(14)`. CNPJ alfanumérico preserva letras e números, normaliza caixa sem conversões, e a unicidade global (ativos/inativos) usa exatamente a representação canônica sem máscara; duplicidade retorna 409 e documento não pode ser reutilizado.
3. PUT completo, desativação lógica e efeito em vendas futuras têm regras e lacunas documentadas.
4. Modelo proposto define tipos, constraints, índices e compatibilidade com UUID, timestamps, schema `erp` e Flyway append-only.
5. API proposta define operações, paginação, filtros, DTOs, respostas, erros, datas, identificadores e documentos.
6. A matriz consolidada é ADMIN/MANAGER: acesso completo; SALES: finalidade comercial, leitura/criação/edição sem desativação; FINANCE: somente leitura, sem filtro por documento e com projeção/DTO próprio sem CPF/CNPJ completo; STOCK_OPERATOR: sem acesso. Autorização e minimização ocorrem no backend.
7. Dados pessoais têm acesso minimizado. Auditoria do MVP limita-se a `createdAt` e `updatedAt`, sem tabela/histórico, `createdBy` ou `updatedBy`; auditoria completa fica como evolução futura.
8. Testes de backend, integração PostgreSQL, segurança, OpenAPI e Angular futuro estão especificados.
9. Cliente inativo permanece consultável para histórico e não pode ser usado em novas vendas; validação na criação/confirmação e snapshots ficam especificados no módulo `sales`.
10. Endereço é objeto opcional, com oito campos opcionais persistidos em colunas de `erp.customers`, `null` quando vazio, CEP brasileiro normalizado para oito dígitos e `countryCode` ISO alpha-2 com padrão `BR` quando endereço é informado.
11. Busca/filtros, paginação zero-based, tamanho 20/máximo 100, sort padrão `name,asc`, allowlist, múltiplos `sort` e erro 400 para campo de ordenação desconhecido estão definidos; FINANCE não pode buscar por documento e recebe apenas projeção minimizada do backend.
12. Nenhum código, migration, dependência ou contrato existente é alterado nesta etapa.

## 12. Decisões pendentes antes de C-02

1. No módulo `sales`, definir em quais operações será verificada a situação ativa do cliente e quais dados serão congelados no snapshot histórico, incluindo endereço de entrega.
2. Definir requisitos LGPD de retenção, correção, exportação e anonimização dos dados pessoais.
3. Documentar na implementação a referência exata à documentação oficial vigente da Receita Federal usada para validar CPF e CNPJ alfanumérico; não derivar algoritmo de fontes não oficiais.

## Referências internas analisadas

- `docs/decisions/0001-modular-monolith.md`, `0002-postgresql-flyway.md`, `0003-oidc-keycloak.md`
- `docs/security-model.md`, `docs/domain-model.md`, `docs/business-rules.md`, `docs/requirements.md`
- `docs/api-contract.yaml`
- `docs/specs/catalog-angular.md`, `docs/specs/products-angular.md`
- `apps/api/src/main/java/com/erp/core/catalog/{api,application,domain}`
- `apps/api/src/main/java/com/erp/core/platform/{error,security,web}`
- `apps/api/src/main/resources/db/migration/V1__foundation_schema.sql` até `V3__catalog_nonblank_constraints.sql`
- `apps/web/src/app/app.routes.ts`, `apps/web/src/app/catalog/`, `apps/web/src/app/auth/`
