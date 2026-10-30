# SDD — Catálogo no Angular

**Status:** modelos e serviços HTTP concluídos; gestão de categorias implementada nesta etapa; interface de produtos permanece pendente.

## 1. Objetivo e contexto observado

Especificação técnica baseada no código auditado. A gestão de categorias foi implementada; as telas de produtos e a integração completa do catálogo continuam pendentes.

O frontend usa componentes standalone, Angular Router, signals, HttpClient com interceptor funcional e autenticação keycloak-js. A landing, callback, acesso negado e início permanecem; a gestão de categorias foi acrescentada com rotas autenticadas e authorities de /me. O visual continua nativo e sem biblioteca de UI.

O backend é um monólito modular. Categorias e produtos estão no pacote `catalog`, com controllers, records de request/response, serviços, repositórios JPA e migrations Flyway. A API local de desenvolvimento indicada pelo frontend é `http://localhost:8082`; o OpenAPI ainda declara `http://localhost:8080` como server. Essa diferença de URL base deve ser reconciliada numa revisão futura do contrato OpenAPI, sem alterar os paths descritos abaixo.

## 2. Escopo

### Incluído

- Página paginada de produtos com filtros de nome, SKU, categoria e estado, respeitando os parâmetros reais da API.
- Cadastro, consulta, edição e desativação lógica de produtos.
- Página paginada de categorias com cadastro, consulta, edição e desativação lógica.
- Seleção de categoria ativa em criação/alteração de produto.
- Estados de carregamento, erro, sucesso, lista vazia, ausência de permissão e sessão inválida.
- Rotas autenticadas e controles de interface coerentes com as authorities retornadas por `/me`.

### Excluído

- Estoque, vendas, preço promocional, imagens/anexos, importação em massa, exportação e qualquer nova regra de negócio.
- Exclusão física e reativação de registros.
- Busca, filtro ou ordenação de categorias que exijam parâmetros inexistentes no backend.
- Autorização baseada somente em componentes/guards Angular.

## 3. Contratos existentes

Base local atual: `http://localhost:8082`; prefixo funcional: `/api/v1`. Todas as rotas abaixo exigem access token Bearer. Não enviar ID token. Respostas de sucesso usam JSON, exceto DELETE que retorna 204 sem corpo.

Na etapa de serviços/modelos, os tipos TypeScript representam `price` como `number`, conforme o JSON atual, e os serviços passam os corpos sem conversão ou arredondamento. Isso não elimina o limite de precisão de JavaScript identificado abaixo; a estratégia decimal segue pendente antes da construção dos formulários.

### 3.1 Categorias

| Operação | Endpoint | Autorização | Resposta de sucesso |
|---|---|---|---|
| Listar | `GET /api/v1/categories?page=0&size=20&sort=name,asc` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `PageResponse<CategoryResponse>` |
| Consultar | `GET /api/v1/categories/{id}` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `CategoryResponse` |
| Criar | `POST /api/v1/categories` | ADMIN, MANAGER | 201 `CategoryResponse`, header `Location` |
| Atualizar | `PUT /api/v1/categories/{id}` | ADMIN, MANAGER | 200 `CategoryResponse` |
| Desativar | `DELETE /api/v1/categories/{id}` | ADMIN, MANAGER | 204 sem body |

A lista inclui categorias ativas e inativas. `page` é zero-based, `size` padrão 20 e máximo 100. `sort` é o parâmetro Spring Pageable; o padrão implementado é `name,asc`. Não há parâmetros de pesquisa ou filtro de categoria.

Request de criação/atualização:

```json
{"name":"Hardware","description":"Peças e componentes"}
```

`name` é obrigatório, não branco, máximo 120 caracteres; backend aplica `trim` e unicidade case-insensitive. `description` é opcional/nulo, máximo 2000. Resposta tem `id` UUID, `name`, `description` nullable, `active`, `createdAt` e `updatedAt` ISO-8601.

### 3.2 Produtos

| Operação | Endpoint | Autorização | Resposta de sucesso |
|---|---|---|---|
| Listar | `GET /api/v1/products` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `PageResponse<ProductResponse>` |
| Consultar | `GET /api/v1/products/{id}` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `ProductResponse` |
| Criar | `POST /api/v1/products` | ADMIN, MANAGER | 201 `ProductResponse`, header `Location` |
| Atualizar | `PUT /api/v1/products/{id}` | ADMIN, MANAGER | 200 `ProductResponse` |
| Desativar | `DELETE /api/v1/products/{id}` | ADMIN, MANAGER | 204 sem body |

Filtros opcionais de listagem: `name` (contains, case-insensitive), `sku` (contains após normalização para maiúsculas), `categoryId` (UUID exato), `active` (boolean). Sem `active`, inclui registros ativos e inativos. Paginação, máximo e ordenação padrão são iguais à listagem de categorias. A UI deve usar paginação do servidor, não paginar somente a página já carregada.

Request de criação/atualização (PUT exige o DTO completo):

```json
{"sku":"AB-120","name":"Parafuso","description":null,"categoryId":"<uuid>","price":12.50}
```

`sku` obrigatório, máximo 64; backend aplica trim e uppercase e exige unicidade. `name` obrigatório, máximo 160 e trimado. `description` opcional/nulo, máximo 2000. `categoryId` é UUID obrigatório. Categoria precisa existir; criação e mudança de associação exigem categoria ativa. `price` é número decimal entre 0 e 999999999999.99, até duas casas; backend persiste `NUMERIC(14,2)`. Resposta contém `id`, `sku`, `name`, `description` nullable, `categoryId`, `price`, `active`, `createdAt` e `updatedAt`.

### 3.3 Paginação e erros

Ambas as listagens retornam:

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}
```

- `400 application/problem+json`: parâmetros/corpo inválidos. Campos inválidos acrescentam `errors`, um mapa de nome de campo para mensagem.
- `401`: bearer ausente/inválido. A configuração atual não faz login OIDC automaticamente ao receber 401; a interface deve apresentar sessão expirada e ação explícita para entrar novamente.
- `403`: principal autenticado sem autoridade para a operação. Exibir mensagem de acesso negado sem repetir a requisição em loop.
- `404 application/problem+json`: UUID de recurso/categoria inexistente.
- `409 application/problem+json`: nome/SKU duplicados, associação a categoria inativa ou outra restrição relacional.
- `201`: criação; `200`: consulta/atualização/listagem; `204`: desativação sem corpo.

Problem Details entrega `type`, `title`, `status` e `detail`; validação do corpo pode incluir `errors`. A UI deve tolerar detalhes sem mensagem de campo e não depender de texto interno de exceções para lógica.

## 4. Regras de domínio que a interface deve preservar

1. Desativação é lógica. `DELETE` não apaga histórico, remove associação ou retorna a entidade; após sucesso atualizar/recarregar a listagem e apresentar estado inativo.
2. Não existe reativação. O request de update não contém `active`; nunca enviar esse campo como mecanismo de ativação. A decisão funcional de reativar registros permanece pendente.
3. Categoria inativa continua consultável e listada. Produtos existentes permanecem vinculados e podem ser atualizados se mantiverem a mesma categoria inativa. Para editar esse produto, manter a categoria atual selecionável/visível como valor existente, mas não oferecer a categoria inativa como nova associação.
4. Produto novo ou produto alterado para outra categoria só pode apontar para categoria ativa. A lista de categorias não oferece filtro de estado; para montar o seletor, buscar páginas com `size=100`, agregar resultados necessários e filtrar por `active` no cliente. Ao editar produto ligado a categoria inativa, preservar apenas essa associação corrente como exceção. Se volume real de categorias exigir carregamento incremental, falta endpoint de categorias ativas/pesquisa e isso exige decisão futura de API.
5. SKU e nome exibidos após salvar refletem a normalização do servidor. Tratar 409 de unicidade como erro associado ao campo quando for possível inferir com segurança; manter também uma mensagem geral.
6. Preço deve aceitar apenas valores não negativos e até duas casas. Não aplicar cálculos monetários com ponto flutuante no frontend. O contrato atual representa `price` como JSON number, que `HttpClient`/JavaScript lê como `number`; a faixa NUMERIC(14,2) pode exceder a representação decimal exata de JavaScript. Antes de prometer fidelidade integral no limite, decidir entre mudança compatível no contrato para decimal como string ou parsing/serialização decimal exata no cliente. Nenhuma biblioteca decimal está instalada.

## 5. Arquitetura Angular proposta

### Organização

Manter componentes standalone e agrupar a feature por responsabilidade, sem criar módulos Angular tradicionais:

```text
src/app/catalog/
  models/                 # Category, Product, PageResponse e DTOs
  data-access/            # CatalogApiService e serialização/Problem Details
  products/               # página/lista, filtros e formulário standalone
  categories/              # página/lista e formulário standalone
  shared/                  # apenas controles realmente reutilizados nesta feature
```

Rotas sugeridas: `/catalog/products`, `/catalog/products/new`, `/catalog/products/:id/edit`, `/catalog/categories`, `/catalog/categories/new` e `/catalog/categories/:id/edit`. Proteger todas com o `authGuard` atual. `@angular/forms` 22.2.3 está instalada e alinhada ao Angular 22.2.3 para Reactive Forms; não adicionar biblioteca visual sem necessidade demonstrada.

### Serviços e estado

- Os serviços de data access da feature concentram URLs, query params tipados, DTOs e métodos HTTP. A primeira etapa usa `CategoryApiService` e `ProductApiService`, compartilhando utilitários de URL/paginação. Usar base `environment.apiBaseUrl` (`http://localhost:8082/api` no dev) e paths relativos `/v1/...`; não duplicar host ou versão em componentes.
- Integrar com `apiTokenInterceptor` existente, que atualiza token e só envia bearer para origem/path da API. Não ler tokens no componente nem registrar headers.
- Usar sinais/estado local de página conforme padrão atual. Cada operação assíncrona deve representar loading, sucesso/lista vazia, erro de rede e status 401/403/404/409.
- Não existe handler global de Problem Details para reutilizar. Criar mapper tipado na feature se necessário, mantendo erro HTTP de negócio local e sem acrescentar interceptor global genérico.
- Estado `active`/filtros deve sempre refletir query do servidor; ao alterar filtro ou ordenação, retornar `page` a zero. Paginação deve enviar os parâmetros recebidos e representar `totalElements`, `totalPages`, `first`, `last`.

### Formulários

Usar Reactive Forms, controles tipados e validators alinhados ao backend. Mostrar erro após interação ou submit, manter valores em caso de falha e marcar campos com `aria-invalid`/`aria-describedby`. No update usar PUT completo, incluindo `description: null` quando vazio e nunca enviar `active`. No produto, carregar categoria antes de habilitar submit e lidar com categoria apagada/inexistente como erro API.

Campos: categoria (`name`, `description`); produto (`sku`, `name`, `description`, `categoryId`, `price`). Max lengths: 120, 2000, 64, 160, 2000; UUID não editável. `price` segue o formato decimal local decidido (ex.: vírgula pt-BR), mas deve ser normalizado sem arredondar nem exceder duas casas. Resolver antes da implementação a serialização exata indicada acima.

## 6. Experiência, acessibilidade e segurança

- Layout responsivo com os padrões CSS nativos existentes, navegação clara por Produtos/Categorias e conteúdo utilizável em viewport estreita. Preferir tabela semântica em desktop e apresentação/lista com os mesmos rótulos em mobile; controles devem funcionar por teclado.
- Produtos: colunas sugeridas SKU, nome, categoria, preço, situação e ações; filtros refletem somente parâmetros reais. Categorias: nome, descrição, situação, atualização e ações. Mostrar confirmação antes de desativar, com o impacto específico: produto associado permanece histórico e pode manter vínculo.
- Formulários com rótulos persistentes, mensagens associadas ao campo e foco inicial/retorno após erro. Sucesso em região `aria-live`; falha relevante com `role=alert`; carregamento com `role=status`; não depender só de cor.
- ADMIN/MANAGER veem ações de escrita; ADMIN/MANAGER/SALES/STOCK_OPERATOR podem consultar; FINANCE não tem acesso ao catálogo. Authorities vêm de `/me`, sem inferir permissão do token no cliente. Se `/me` falhar, não mostrar ações de escrita. A API continua a fonte de autorização e deve responder 403 para qualquer tentativa proibida.
- Não expor token, refresh token, conteúdo de headers, nem detalhes internos em mensagens/logs. Manter CORS e autenticação atuais; UI não pode contorná-los.

## 7. Plano incremental de implementação

1. **[Concluída] Modelos e data access:** tipos DTO, página genérica, `CategoryApiService` e `ProductApiService` implementados com testes HTTP locais. A URL, query params, corpos, retorno 204 e propagação de Problem Details estão cobertos. A integração ao interceptor decorre do `HttpClient` configurado no app; autorização real permanece no backend. A representação de `price` é `number` sem conversão no serviço, com precisão integral ainda pendente para a futura UI.
2. **[Concluída] Gestão de categorias:** listagem autenticada, paginação/ordenação, status active/inactive, estados vazio/erro e cobertura automatizada.
3. **[Concluída] Escrita de categorias:** criação, edição PUT completo, validações, desativação lógica confirmada e autorização visual por authorities; veja a conclusão desta etapa.
4. **Leitura de produtos:** paginação server-side e filtros name/SKU/categoria/active, limpeza e combinação de filtros, ordenação `name,asc`, testes dos query params e telas vazia/loading/erro.
5. **Formulário de produto:** criar/editar, seleção de categorias ativas, preservação de associação atual inativa, precisão decimal, conflito de SKU e categoria inválida; testes de formulário/serviço.
6. **[Concluída para categorias] Desativação e autorização visual:** confirmação, DELETE/204, atualização e matriz de acesso; produtos continuam pendentes.
7. **Validação integrada:** lint, todos os testes, build Angular, smoke manual com usuário autorizado e conta autenticada sem permissão se disponível; verificar 401, 403 e comportamento de rede real sem registrar tokens. Confirmar cada passo antes de iniciar o próximo.

## 8. Estratégia de testes

- Modelos/serializador de API: conversão de `PageResponse`, nulabilidade e round-trip de preço nos limites; não arredondar valores.
- `CatalogApiService`: endpoint correto, query param omitido quando vazio, encoding de texto/UUID/boolean, paginação/ordenação, response body e códigos HTTP.
- Formulários: required/not-blank, maxLength, formato decimal, precisão, categoria obrigatória e prevenção de reativação por payload.
- Componentes: carregamento, vazio, erro, retry, paginação, filtros reiniciando página, confirmação e mensagens acessíveis.
- Segurança no cliente: ações visuais para cada authority, estado de `/me` indisponível; assertiva explícita de que esconder controle não é autorização.
- Testes atuais: Angular unit-test builder/Vitest, `HttpTestingController`, ESLint e Prettier. Continuar padrão existente, sem substituir suíte por E2E novo sem avaliar custo.
- Validação manual: com Keycloak e API locais, autenticar por OIDC PKCE já configurado; validar listagem/criação/edição/desativação com ADMIN/MANAGER, leitura com SALES ou STOCK_OPERATOR, 403 com FINANCE e 401 sem sessão. Não copiar token/cookies para evidências. Confirmar também 400 de campo, 409 de duplicidade, 404 de item ausente, páginas vazias e falha offline.

## 9. Critérios de aceite

1. Usuário não autenticado não acessa nenhuma rota de catálogo; tentativa direta não carrega dados e a API permanece protegida.
2. Leitores autorizados navegam pelas páginas; FINANCE não recebe acesso ao catálogo e a API confirma 403 para operações vedadas.
3. Produtos paginam no servidor e combinam apenas filtros documentados; `page`, `size`, ordenação e contagens correspondem aos parâmetros/resposta da API.
4. Categorias paginam no servidor e apresentam estado de cada registro, sem filtro de categoria inventado.
5. ADMIN/MANAGER criam e editam produto/categoria com os campos e limites do backend; sucesso usa resposta do servidor como estado persistido.
6. Categoria inativa não é oferecida como nova associação; edição de produto já associado pode preservar exatamente essa associação, sem permitir trocá-la por inativa.
7. Desativação pede confirmação, trata 204 sem corpo e atualiza estado como inativo; nenhuma operação apaga fisicamente ou reativa registro.
8. API 400/401/403/404/409, falha de rede, lista vazia e loading têm feedback acessível e não deixam estado antigo enganoso.
9. Valores de preço não são arredondados silenciosamente; serialização e apresentação passam testes nos limites acordados e na precisão de duas casas.
10. Lint, testes e build frontend passam; smoke manual confirma fluxos autorizados e proibidos sem exposição de tokens.

## 10. Lacunas, riscos e decisões pendentes

- **Contrato OpenAPI:** `servers.url` ainda aponta para `http://localhost:8080`, enquanto ambiente local atual usa 8082. Confirmar se 8082 deve virar a URL oficial de desenvolvimento e ajustar contrato em uma tarefa própria.
- **Preço:** JSON number + JavaScript `number` não garante decimal exato em toda a faixa NUMERIC(14,2). Decidir serialização decimal antes de implementar o formulário para não introduzir arredondamento ou uma alteração de contrato inadvertida.
- **Categorias em formulários:** endpoint não filtra ativos nem pesquisa; carregamento de todas as páginas é estratégia inicial. Validar volume esperado ou priorizar filtro `active=true` e busca no backend.
- **Reativação:** não existe endpoint; aguarda decisão funcional, sem botão/recurso de reativar.
- **Ordenação:** o backend aceita `sort` de Pageable e documenta `name,asc` como padrão, mas não há allowlist de propriedades de ordenação no contrato. Na primeira entrega, limitar seletor de ordenação à opção verificada `name`; expandir somente após teste backend.
- **Mensagens:** conteúdo de `detail` para conflito é texto, não código estável. A UI deve oferecer fallback genérico; uma enumeração/código de erro exigiria evolução futura do backend.
- **Design system:** não há biblioteca visual nem decisões formais de branding. Usar componentes nativos e estilos existentes nesta primeira entrega; discutir adoção de biblioteca separadamente se surgirem requisitos concretos.
- **Smoke real:** a integração OIDC real foi validada para `/me` conforme contexto do solicitante, mas cada operação do catálogo ainda precisa de smoke humano com roles apropriadas e ambiente corrente.

## 11. Fontes auditadas

## 12. Conclusão da etapa de gestão de categorias

**Status:** modelos e serviços HTTP concluídos; gestão de categorias implementada nesta etapa; interface de produtos permanece pendente.

- Rotas: `/catalog/categories`, `/catalog/categories/new` e `/catalog/categories/:id/edit`. Todas usam `authGuard` e guardas adicionais baseadas nas authorities retornadas por `GET /api/v1/platform/me`.
- Leitura: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_SALES` e `ROLE_STOCK_OPERATOR`. Escrita: somente `ROLE_ADMIN` e `ROLE_MANAGER`. Ausência ou falha ao carregar `/me` falha fechada. O backend continua responsável pela autorização efetiva.
- A lista usa `GET /api/v1/categories` com `page` zero-based, `size=20` (máximo permitido 100) e `sort=name,asc`; não inventa busca ou filtro. Categorias inativas são identificadas visualmente.
- Formulário reativo usa `name` (obrigatório, não branco, máximo 120) e `description` (opcional/nula, máximo 2000). Criação usa POST; edição carrega por UUID e usa PUT completo. O payload não inclui `active`; não há reativação.
- Desativação confirma com o usuário, envia DELETE, aceita 204 sem corpo e atualiza a página; erros 403/409 recebem mensagens de interface. Erros 400/409 do formulário preservam os campos preenchidos.
- Navegação global inclui link para Categorias quando autenticado. Não foi adicionada biblioteca visual. `@angular/forms` 22.2.3 foi incluída como dependência runtime alinhada ao Angular 22.2.3 para Reactive Forms; lockfile regenerado.
- Testes adicionados para listagem, paginação, estado vazio, falha HTTP, confirmação, 204, formulários POST/PUT, validação/conflito, restrição de escrita e guards/rotas. Resultados realmente executados estão em `docs/validation.md`.

**Pendências:** validar manualmente com contas Keycloak apropriadas (ADMIN/MANAGER, leitor e FINANCE); ainda não há tela de consulta individual para perfis somente leitores. Reativação permanece pendente de decisão funcional.

- Frontend: `apps/web/src/app/app.routes.ts`, `app.component.ts`, `pages/home.component.ts`, `pages/landing.component.ts`, `auth/auth.guard.ts`, `auth/auth.service.ts`, `auth/identity-api.service.ts`, `auth/api-token.interceptor.ts`, testes correspondentes, `styles.css`, `package.json`, `angular.json` e ambientes.
- Backend: `catalog/api/*Controller.java`, request/response records e `PageResponse`; `catalog/application/CategoryService.java`, `ProductService.java`, `CatalogExceptions.java`; `catalog/domain/Category.java`, `Product.java`, repositories; `platform/error/ApiExceptionHandler.java`, `platform/security/SecurityConfiguration.java`, `platform/web/PaginationConfiguration.java`; migrations V2/V3 e `CatalogIntegrationTest.java`.
- Contrato e decisões: `docs/api-contract.yaml`, `docs/business-rules.md`, `docs/security-model.md`, `docs/domain-model.md`, `docs/requirements.md`, `docs/backlog-phase-1.md` e `docs/validation.md`.
