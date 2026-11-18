# SDD — Gestão de produtos no Angular

**Status:** modelos TypeScript e serviço HTTP de produtos consomem a API v2. Formulários e componentes visuais de produtos continuam pendentes.

## 1. Contexto e objetivo

Especificar a gestão de produtos no frontend Angular reaproveitando o data access do catálogo, modelos de categoria/paginação, autenticação, guards, rotas standalone, sinais, Reactive Forms e estilos nativos existentes. O serviço de produtos usa `/api/v2/products`; categorias continuam em `/api/v1/categories`. A base local vem de `environment.apiBaseUrl` (`http://localhost:8082/api`).

O escopo inclui listagem paginada e filtrada, consulta, cadastro, edição, seleção de categoria, preço BRL e desativação lógica. Não inclui estoque, imagens, promoções, exclusão física, reativação ou campos/endpoints não presentes no backend.

## 2. Contratos reais do backend

Fontes: controllers v1/v2, DTOs `ProductV2Request`/`ProductV2Response`, `ProductService`, `PageResponse`, entidade `Product`, repositórios, `SecurityConfiguration` e migrations V1–V3. A API v1 segue disponível sem alteração e continua com preço numérico; o Angular deste catálogo usa a v2, cujo preço é string decimal. Todas as rotas `/api/**` exigem bearer JWT válido com issuer/audience aceitos; a configuração é stateless.

| Operação | Contrato | Autoridade | Sucesso |
|---|---|---|---|
| Listar | `GET /api/v2/products?page=0&size=20&sort=name,asc` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `PageResponse<ProductV2Response>` |
| Consultar | `GET /api/v2/products/{id}` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `ProductV2Response` |
| Criar | `POST /api/v2/products` | ADMIN, MANAGER | 201 `ProductV2Response` e `Location` |
| Atualizar | `PUT /api/v2/products/{id}` | ADMIN, MANAGER | 200 `ProductV2Response` |
| Desativar | `DELETE /api/v2/products/{id}` | ADMIN, MANAGER | 204 vazio |

Filtros opcionais reais do GET: `name` (contains, case-insensitive), `sku` (contains após trim/uppercase), `categoryId` (UUID exato), `active` (boolean). Filtros são combinados por AND. `name`/`sku` vazios ou brancos são ignorados. Sem `active`, a página inclui ativos e inativos. A pesquisa escapa `%`, `_` e `\\` como caracteres literais. Spring Pageable usa página zero-based, padrão 20, ordenação padrão por `name` ascendente; não há limite máximo de `size` explícito no controller auditado. `sort` usa a sintaxe Spring e não tem allowlist específica no controller. UI deve começar com `name,asc`, sem oferecer ordenações adicionais até serem verificadas.

Corpo completo de criação e PUT:

```json
{"sku":"AB-120","name":"Parafuso","description":null,"categoryId":"<uuid>","price":"12.50"}
```

`sku`: obrigatório, não branco, até 64 caracteres; serviço aplica trim e uppercase, unicidade exata no valor normalizado. `name`: obrigatório, não branco, até 160; serviço aplica trim. `description`: opcional/nulo, até 2000. `categoryId`: UUID obrigatório e existente. Criação requer categoria ativa. Em edição, categoria inativa pode permanecer somente se for a associação atual; troca para categoria inativa resulta em conflito. Na v2, `price` deve ser string canônica que corresponda a `(?:0|[1-9][0-9]{0,11})\\.[0-9]{2}`: mínimo `0.00`, máximo `999999999999.99`, exatamente duas casas e sem arredondamento. O backend converte diretamente para `BigDecimal` e persiste em `NUMERIC(14,2)`. A v1 mantém seu DTO e número JSON.

Resposta v2: `id` UUID, `sku`, `name`, `description` nullable, `categoryId` UUID, `price` string decimal com duas casas, `active`, `createdAt`, `updatedAt` (`Instant`/ISO-8601). `active` não é aceito no request. A página contém `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

Migrations: V1 cria schema `erp`; V2 cria `erp.products` e `erp.categories`, FK `category_id`, SKU único, índice por categoria e índice `(active, lower(name))`, checks de preço não negativo e SKU normalizado; V3 exige nome trimado/não vazio e SKU não vazio. O preço no Java usa `BigDecimal` na request, domínio e response.

Erros observáveis documentados no projeto: 400 para validação/parâmetro inválido (Problem Details e `errors` por campo quando aplicável), 401 bearer inválido/ausente, 403 autoridade insuficiente, 404 produto/categoria ausente, 409 SKU duplicado ou categoria inativa vedada. O texto de `detail` não é código estável; mensagens de UI devem ter fallback e não usar texto para regras de negócio. DELETE é desativação lógica (`active=false`), mantém registro/histórico e responde 204 sem body; não existe reativação.

## 3. Precisão monetária e decisão

O armazenamento `NUMERIC(14,2)` comporta até `999999999999.99`. Java `BigDecimal` mantém decimal exato. Na v1, o preço continua sendo número JSON e pode perder precisão ao ser interpretado como JavaScript `number`. A API v2 entrega e recebe o preço como string decimal; os modelos e o serviço Angular agora preservam essa string sem conversão monetária. Esta etapa não cria formulário, parser de entrada nem formatação BRL.

| Alternativa | Precisão ponta a ponta | Impacto |
|---|---|---|
| 1. JSON number + TS `number` | Não garantida em toda a faixa; validação decimal no backend permanece boa, mas não recupera dígitos já perdidos no browser. `Intl.NumberFormat` só formata o valor aproximado recebido. | Mantém contrato/consumidores; risco de arredondamento silencioso. |
| 2. Decimal JSON string + Java `BigDecimal` | Sim, se frontend trata como string decimal e não converte a `number`; pode validar/formatar com parser decimal exato. | Mudança incompatível para clientes que esperam number; Jackson pode aceitar string numérica para `BigDecimal`, mas resposta muda de tipo. Exige contrato explícito, OpenAPI, testes de serialização e coordenação/versionamento de consumidores. |
| 3. Biblioteca decimal no frontend com contrato atual | Entrada/cálculo pode ser exato após parsing do texto, mas o JSON number já pode chegar aproximado e a serialização via `number` pode perder precisão. Uma biblioteca não restaura informação perdida na resposta. | Dependência nova e complexidade; não resolve sozinha o contrato atual. |

**Decisão implementada:** a API v2 usa `price` como string decimal canônica em request e response, mantendo Java `BigDecimal` e PostgreSQL `NUMERIC(14,2)`. O Angular usa `price: string` e envia produtos para `/api/v2/products`; não converte preço para `number`. A v1 não foi alterada. Formatação brasileira permanece uma decisão de apresentação para a etapa de UI; nenhum formatter ou formulário é criado nesta etapa.

Não há biblioteca decimal instalada ou necessária para esta integração HTTP: o cliente transporta os valores como strings. Se cálculos monetários no frontend forem introduzidos futuramente, devem ser especificados sem conversão para ponto flutuante.

## 4. Arquitetura e interface Angular

Reutilizar `ProductApiService` (`list/get/create/update/deactivate`), `CategoryApiService`, `catalogProductV2Url`, `catalogResourceUrl` (categorias v1), `pageableHttpParams`, `appendNonEmptyParam`, `ProductListQuery`, `Product`, `ProductWriteRequest`, `PageResponse`, interceptor de token e padrões de `CategoryListComponent`/`CategoryFormComponent`. Os serviços enviam os filtros existentes e DELETE como `Observable<void>`. Modelos de resposta/criação/atualização usam `price: string`; serviço não converte nem formata o valor.

Rotas a acrescentar em `app.routes.ts`: `/catalog/products`, `/catalog/products/new`, `/catalog/products/:id/edit`, protegidas por `authGuard` e guards de leitura/escrita conforme categoria. Incluir navegação Produtos junto de Categorias. Leitura para `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_SALES`, `ROLE_STOCK_OPERATOR`; escrita e ações visíveis apenas para `ROLE_ADMIN`, `ROLE_MANAGER`. Guard/visibilidade usa authorities carregadas por `/me` e falha fechada se indisponível. API continua sendo autoridade final; FINANCE não aparece na allowlist.

**Lista:** tabela acessível com SKU, nome, categoria, preço, situação e ações. Filtros de nome, SKU, categoria e ativo/inativo, mais opção “todos” que omite `active`. Aplicar filtros combinados no servidor, reiniciar página para zero ao mudar filtro, debounce razoável para texto e preservar filtros ao navegar. Paginar por resposta do servidor, com anterior/próxima, página atual e `totalElements`. Estados distintos de carregamento, erro/retry, vazio sem resultados e ausência de registros; tratar 401/403 sem repetição automática. Ordenação inicial `name,asc`.

**Categoria:** seletor por nome usando categorias ativas. A API não tem busca/filtro active para categorias; carregar páginas via `CategoryApiService.list` e filtrar `active` no cliente como solução inicial, sem presumir que uma página contém todas. Ao editar produto que já referencia categoria inativa, incluir a categoria atual como opção de manutenção e não oferecê-la para novas associações. Produto com categoria apagada ou não carregável apresenta erro recuperável e não envia formulário inválido. Volume de categorias pode exigir endpoint futuro de categorias ativas/busca.

**Cadastro/edição:** standalone com Reactive Forms tipados. Ao editar, obter `ProductApiService.get(id)` e preencher formulário. Campos editáveis são SKU, nome, descrição, categoria e preço; ID/active/auditoria são resposta apenas. PUT envia todos os campos do DTO, usando `description: null` quando vazio e sem `active`. Exibir valores normalizados retornados pelo servidor após persistência. Estados loading, saving, sucesso, erro com valores preservados, 404 e permissão insuficiente.

**Preço:** entrada pt-BR aceita vírgula como separador decimal, não aceita negativo, mais de duas casas ou valor superior a `999999999999.99`; não arredondar. Exibir duas casas e prefixo R$ em lista/detalhe. Conversão para o wire format deve ser textual/exata após a evolução recomendada. Se o backend rejeitar 400, mapear campo quando `errors.price` existir; caso contrário, apresentar mensagem geral de validação.

**SKU e validações:** required e não branco após trim; máximo 64. Normalização uppercase/trim pode ser mostrada como ajuda, mas servidor é fonte da forma canônica. 409 de duplicidade associa erro a SKU se a operação/contexto e detalhe permitirem inferir, além de mensagem geral; nunca depender de frase exata. Nome required/não branco e máximo 160; descrição máximo 2000; categoria obrigatória e elegível; preço requerido, mínimo zero, duas casas e limite de 12 dígitos inteiros. Mostrar erros após interação/submit, rótulos persistentes, `aria-invalid`, `aria-describedby`, foco acessível, sucesso em `aria-live` e erro relevante em `role=alert`.

**Desativação:** apenas para produto ativo e perfil escritor. Confirmar ação e explicar que o registro permanece no histórico; chamar DELETE, aceitar 204 sem body, recarregar página/estado e mostrar inativo. Não oferecer reativação. Se a página ficar vazia após remover o item, ajustar para página válida anterior ou exibir vazia.

## 5. Evoluções necessárias no backend

1. O contrato v2 string, DTOs próprios, validação, conversão direta a `BigDecimal` e response com duas casas já existem no backend. Angular data access e modelos agora consomem v2; manter testes de contrato e transporte sem conversão monetária.
2. `Pageable` não define `size` máximo nem allowlist de sort no controller. Para limitar carga e superfície, avaliar tamanho máximo explícito e ordenações suportadas, retornando 400 previsível para valor inválido. Até então UI envia apenas size 20 e `name,asc`.
3. Se número/volume de categorias crescer, adicionar endpoint/filtros de categoria ativa com pesquisa ou paginação apropriada para seletor. Atual serviço de categorias não filtra.
4. Respostas de conflito usam `detail` textual. Para erros estáveis por campo, considerar códigos Problem Details como `SKU_ALREADY_EXISTS` e `CATEGORY_INACTIVE`; não é requisito para a UI inicial.
5. OpenAPI/configuração da URL local diverge segundo a spec de catálogo (server documentado 8080 e environment web 8082); reconciliar em revisão contratual separada.

Não é necessária evolução de authorities para a tela: controller já restringe leitura/escrita conforme a matriz descrita.

## 6. Plano incremental

1. **[Concluída nesta etapa]** Migrar modelos e `ProductApiService` para API v2, mantendo `CategoryApiService` na v1; cobrir URL, query params, bodies string, 204 e erros HTTP.
2. Construir listagem standalone, filtros reais, paginação no servidor, estados e guards; cobrir serviço e componente.
3. Implementar carregamento e seletor de categoria, incluindo preservação da categoria inativa já associada.
4. Implementar cadastro/edição com validações de SKU/nome/categoria/preço e tratamento de 400/401/403/404/409.
5. Implementar desativação lógica com confirmação e resposta 204 na interface.
6. Implementar apresentação/formatação BRL exata na UI, sem conversão para ponto flutuante, e executar testes/build/smoke autenticado com papéis adequados.

## 7. Plano de testes

- **API Angular:** URL `/api/v2/products`, filtros `name`, `sku`, `categoryId`, `active`, omissão de opcionais vazios, combinações, paginação zero-based, `size`, `sort=name,asc`; validar resposta PageResponse e 400/401/403/404/409. Preço em respostas e request bodies permanece string, incluindo `999999999999.99`.
- **Lista:** filtros combinados e reset para página zero; navegação anterior/próxima, totais, vazio, loading, retry, erro e categoria exibida. Confirmar que filtro “todos” omite `active`.
- **Formulário:** GET de edição; POST e PUT com DTO completo; normalização/trim de SKU, required, branco, maxLength 64, nome 160, descrição 2000; SKU duplicado/409; categoria obrigatória, ativa para nova associação e preservada inativa para associação existente.
- **Preço na camada HTTP:** transportar strings como `0.01`, `10.99` e `999999999999.99` sem conversão; confirmar round-trip exato nos modelos/serviço. Formato pt-BR, validação de entrada e apresentação ficam para a etapa de formulários.
- **Desativação:** confirmação cancelada não chama serviço; confirmação chama DELETE; resposta 204 vazio aceita; estado é recarregado/inativo; não há ação de reativação.
- **Permissões:** ADMIN/MANAGER leem e escrevem; SALES/STOCK_OPERATOR leem sem controles de escrita; FINANCE e usuário sem role não acessam rota; `/me` indisponível falha fechado; backend retorna 403 em tentativas proibidas. Esconder botão não substitui teste da API.

## 8. Critérios de aceite

1. Leitores autorizados consultam lista e detalhe; perfis sem leitura são barrados por guard e API.
2. Lista combina somente filtros reais e pagina no servidor, preservando totalizadores e estado dos filtros.
3. ADMIN/MANAGER criam/atualizam usando todos e somente os campos de `ProductV2Request`; SKU/name refletem a resposta normalizada e preço mantém tipo string.
4. Categoria inativa não pode ser nova associação; edição pode manter somente o vínculo inativo corrente.
5. O transporte v2 mantém cada centavo até o máximo de `NUMERIC(14,2)` sem conversão ou arredondamento. A exibição BRL será aceita na etapa visual.
6. Desativação é lógica, confirma intenção, trata 204 sem corpo e não oferece exclusão permanente ou reativação.
7. Loading, vazio, erros HTTP e falhas de rede têm resposta acessível, sem apagar valores digitados em falha de gravação.
8. Testes automatizados cobrem paginação/filtros, criação/edição, validações, preço e limite, desativação, 204 e permissões.

## 9. Riscos e decisões pendentes

- **Contrato v1:** permanece ativo e numérico para clientes existentes. O frontend deste catálogo usa v2; não misturar modelos v1 e v2 no mesmo serviço.
- **Categorias inativas:** associação atual pode permanecer na edição. Confirmar texto/UX e estratégia de carregamento quando houver grande volume.
- **Tamanho/sort:** backend hoje não limita size nem restringe propriedade de ordenação; decidir limites e allowlist.
- **Reativação:** sem endpoint e fora do escopo; requer decisão de domínio/API futura.
- **OpenAPI/base local:** divergência 8080/8082 herdada da spec catalog-angular.
- **Texto de conflito:** é detalhe textual e pode mudar; códigos estruturados são evolução futura.

## 10. Arquivos auditados

- `docs/specs/catalog-angular.md`
- `apps/web/src/app/catalog/models/catalog.models.ts`
- `apps/web/src/app/catalog/data-access/product-api.service.ts`
- `apps/web/src/app/catalog/data-access/category-api.service.ts`
- `apps/web/src/app/catalog/data-access/catalog-api.utils.ts`
- `apps/web/src/app/catalog/data-access/product-api.service.spec.ts`
- `apps/web/src/app/catalog/categories/category-access.guard.ts`
- `apps/web/src/app/catalog/categories/category-list.component.ts` e `category-form.component.ts`
- `apps/web/src/app/app.routes.ts` e `apps/web/src/app/app.component.ts`
- `apps/api/src/main/java/com/erp/core/catalog/api/ProductController.java`, `ProductRequest.java`, `ProductResponse.java`, `PageResponse.java`
- `apps/api/src/main/java/com/erp/core/catalog/application/ProductService.java`
- `apps/api/src/main/java/com/erp/core/catalog/domain/Product.java`, `ProductRepository.java`
- `apps/api/src/main/java/com/erp/core/platform/security/SecurityConfiguration.java`
- `apps/api/src/main/resources/db/migration/V1__foundation_schema.sql`, `V2__catalog_categories_and_products.sql`, `V3__catalog_nonblank_constraints.sql`
- `apps/web/src/app/catalog/data-access/product-api.service.spec.ts` e testes de categoria; `apps/api/src/test/java/com/erp/core/catalog/CatalogIntegrationTest.java`
