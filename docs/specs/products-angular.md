# SDD — Gestão de produtos no Angular

**Status:** especificação baseada no backend e no catálogo Angular existentes. A interface de produtos ainda não está implementada. Este documento não altera contratos nem código funcional.

## 1. Contexto e objetivo

Especificar a gestão de produtos no frontend Angular reaproveitando o data access do catálogo já implementado, os modelos de categoria/paginação, autenticação, guards, rotas standalone, sinais, Reactive Forms e estilos nativos existentes. A API fica em `/api/v1`; no ambiente web local atual, `environment.apiBaseUrl` é `http://localhost:8082/api`.

O escopo inclui listagem paginada e filtrada, consulta, cadastro, edição, seleção de categoria, preço BRL e desativação lógica. Não inclui estoque, imagens, promoções, exclusão física, reativação ou campos/endpoints não presentes no backend.

## 2. Contratos reais do backend

Fontes: `ProductController`, `ProductService`, `ProductRequest`, `ProductResponse`, `PageResponse`, entidade `Product`, repositórios, `SecurityConfiguration` e migrations V1–V3. Todas as rotas `/api/**` exigem bearer JWT válido com issuer/audience aceitos; a configuração é stateless. O controller aplica as authorities listadas abaixo.

| Operação | Contrato | Autoridade | Sucesso |
|---|---|---|---|
| Listar | `GET /api/v1/products?page=0&size=20&sort=name,asc` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `PageResponse<ProductResponse>` |
| Consultar | `GET /api/v1/products/{id}` | ADMIN, MANAGER, SALES, STOCK_OPERATOR | 200 `ProductResponse` |
| Criar | `POST /api/v1/products` | ADMIN, MANAGER | 201 `ProductResponse` e `Location` |
| Atualizar | `PUT /api/v1/products/{id}` | ADMIN, MANAGER | 200 `ProductResponse` |
| Desativar | `DELETE /api/v1/products/{id}` | ADMIN, MANAGER | 204 vazio |

Filtros opcionais reais do GET: `name` (contains, case-insensitive), `sku` (contains após trim/uppercase), `categoryId` (UUID exato), `active` (boolean). Filtros são combinados por AND. `name`/`sku` vazios ou brancos são ignorados. Sem `active`, a página inclui ativos e inativos. A pesquisa escapa `%`, `_` e `\\` como caracteres literais. Spring Pageable usa página zero-based, padrão 20, ordenação padrão por `name` ascendente; não há limite máximo de `size` explícito no controller auditado. `sort` usa a sintaxe Spring e não tem allowlist específica no controller. UI deve começar com `name,asc`, sem oferecer ordenações adicionais até serem verificadas.

Corpo completo de criação e PUT:

```json
{"sku":"AB-120","name":"Parafuso","description":null,"categoryId":"<uuid>","price":12.50}
```

`sku`: obrigatório, não branco, até 64 caracteres; serviço aplica trim e uppercase, unicidade exata no valor normalizado. `name`: obrigatório, não branco, até 160; serviço aplica trim. `description`: opcional/nulo, até 2000. `categoryId`: UUID obrigatório e existente. Criação requer categoria ativa. Em edição, categoria inativa pode permanecer somente se for a associação atual; troca para categoria inativa resulta em conflito. `price`: `BigDecimal` obrigatório, mínimo zero, até 12 dígitos inteiros e 2 fracionários (`@Digits`), persistido em `NUMERIC(14,2)` com constraint de não negativo.

Resposta de produto: `id` UUID, `sku`, `name`, `description` nullable, `categoryId` UUID, `price`, `active`, `createdAt`, `updatedAt` (`Instant`/ISO-8601). `active` não é aceito no request. A página contém `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

Migrations: V1 cria schema `erp`; V2 cria `erp.products` e `erp.categories`, FK `category_id`, SKU único, índice por categoria e índice `(active, lower(name))`, checks de preço não negativo e SKU normalizado; V3 exige nome trimado/não vazio e SKU não vazio. O preço no Java usa `BigDecimal` na request, domínio e response.

Erros observáveis documentados no projeto: 400 para validação/parâmetro inválido (Problem Details e `errors` por campo quando aplicável), 401 bearer inválido/ausente, 403 autoridade insuficiente, 404 produto/categoria ausente, 409 SKU duplicado ou categoria inativa vedada. O texto de `detail` não é código estável; mensagens de UI devem ter fallback e não usar texto para regras de negócio. DELETE é desativação lógica (`active=false`), mantém registro/histórico e responde 204 sem body; não existe reativação.

## 3. Precisão monetária e decisão

O armazenamento `NUMERIC(14,2)` comporta até `999999999999.99`. Java `BigDecimal` mantém decimal exato, e `@Digits(integer=12,fraction=2)` rejeita mais de duas casas e mais que 12 dígitos inteiros. O contrato JSON atual, entretanto, representa `price` como número; Jackson normalmente serializa `BigDecimal` como token JSON numérico. O `HttpClient` decodifica esse token como JavaScript `number`, que não representa exatamente todos os centavos em toda a faixa. Um arredondamento pode ocorrer antes do próximo envio, mesmo que Java e PostgreSQL sejam exatos.

| Alternativa | Precisão ponta a ponta | Impacto |
|---|---|---|
| 1. JSON number + TS `number` | Não garantida em toda a faixa; validação decimal no backend permanece boa, mas não recupera dígitos já perdidos no browser. `Intl.NumberFormat` só formata o valor aproximado recebido. | Mantém contrato/consumidores; risco de arredondamento silencioso. |
| 2. Decimal JSON string + Java `BigDecimal` | Sim, se frontend trata como string decimal e não converte a `number`; pode validar/formatar com parser decimal exato. | Mudança incompatível para clientes que esperam number; Jackson pode aceitar string numérica para `BigDecimal`, mas resposta muda de tipo. Exige contrato explícito, OpenAPI, testes de serialização e coordenação/versionamento de consumidores. |
| 3. Biblioteca decimal no frontend com contrato atual | Entrada/cálculo pode ser exato após parsing do texto, mas o JSON number já pode chegar aproximado e a serialização via `number` pode perder precisão. Uma biblioteca não restaura informação perdida na resposta. | Dependência nova e complexidade; não resolve sozinha o contrato atual. |

**Recomendação:** evoluir o contrato para `price` como string decimal canônica em request e response, mantendo Java `BigDecimal` e PostgreSQL `NUMERIC(14,2)`. Exemplo: `"price":"999999999999.99"`. Fazer isso como mudança coordenada e explicitamente versionada/compatibilizada (preferencialmente nova versão de API ou rollout com contrato dual bem definido); não trocar silenciosamente o tipo na API v1. No Angular, manter o valor de edição como string decimal, validar até duas casas e limite, sem passagem por `number`; usar `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })` somente depois de construir representação decimal segura ou por formatter decimal exato. Não serializar via `Number`.

Se for obrigatório implementar primeiro contra o contrato v1 atual, limitar o frontend a texto decimal, evitar converter em `number` antes do POST/PUT e registrar que o parse do response `number` não assegura fidelidade no limite. Não declarar isso como solução plenamente precisa. Nenhuma biblioteca decimal está instalada no catálogo atual.

## 4. Arquitetura e interface Angular

Reutilizar `ProductApiService` (`list/get/create/update/deactivate`), `CategoryApiService`, `catalogResourceUrl`, `pageableHttpParams`, `appendNonEmptyParam`, `ProductListQuery`, `Product`, `ProductWriteRequest`, `PageResponse`, interceptor de token e padrões de `CategoryListComponent`/`CategoryFormComponent`. Os serviços já enviam os filtros reais e DELETE como `Observable<void>`. O modelo atual tipa `price` como `number`; mudar para string somente junto da evolução contratual acordada.

Rotas a acrescentar em `app.routes.ts`: `/catalog/products`, `/catalog/products/new`, `/catalog/products/:id/edit`, protegidas por `authGuard` e guards de leitura/escrita conforme categoria. Incluir navegação Produtos junto de Categorias. Leitura para `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_SALES`, `ROLE_STOCK_OPERATOR`; escrita e ações visíveis apenas para `ROLE_ADMIN`, `ROLE_MANAGER`. Guard/visibilidade usa authorities carregadas por `/me` e falha fechada se indisponível. API continua sendo autoridade final; FINANCE não aparece na allowlist.

**Lista:** tabela acessível com SKU, nome, categoria, preço, situação e ações. Filtros de nome, SKU, categoria e ativo/inativo, mais opção “todos” que omite `active`. Aplicar filtros combinados no servidor, reiniciar página para zero ao mudar filtro, debounce razoável para texto e preservar filtros ao navegar. Paginar por resposta do servidor, com anterior/próxima, página atual e `totalElements`. Estados distintos de carregamento, erro/retry, vazio sem resultados e ausência de registros; tratar 401/403 sem repetição automática. Ordenação inicial `name,asc`.

**Categoria:** seletor por nome usando categorias ativas. A API não tem busca/filtro active para categorias; carregar páginas via `CategoryApiService.list` e filtrar `active` no cliente como solução inicial, sem presumir que uma página contém todas. Ao editar produto que já referencia categoria inativa, incluir a categoria atual como opção de manutenção e não oferecê-la para novas associações. Produto com categoria apagada ou não carregável apresenta erro recuperável e não envia formulário inválido. Volume de categorias pode exigir endpoint futuro de categorias ativas/busca.

**Cadastro/edição:** standalone com Reactive Forms tipados. Ao editar, obter `ProductApiService.get(id)` e preencher formulário. Campos editáveis são SKU, nome, descrição, categoria e preço; ID/active/auditoria são resposta apenas. PUT envia todos os campos do DTO, usando `description: null` quando vazio e sem `active`. Exibir valores normalizados retornados pelo servidor após persistência. Estados loading, saving, sucesso, erro com valores preservados, 404 e permissão insuficiente.

**Preço:** entrada pt-BR aceita vírgula como separador decimal, não aceita negativo, mais de duas casas ou valor superior a `999999999999.99`; não arredondar. Exibir duas casas e prefixo R$ em lista/detalhe. Conversão para o wire format deve ser textual/exata após a evolução recomendada. Se o backend rejeitar 400, mapear campo quando `errors.price` existir; caso contrário, apresentar mensagem geral de validação.

**SKU e validações:** required e não branco após trim; máximo 64. Normalização uppercase/trim pode ser mostrada como ajuda, mas servidor é fonte da forma canônica. 409 de duplicidade associa erro a SKU se a operação/contexto e detalhe permitirem inferir, além de mensagem geral; nunca depender de frase exata. Nome required/não branco e máximo 160; descrição máximo 2000; categoria obrigatória e elegível; preço requerido, mínimo zero, duas casas e limite de 12 dígitos inteiros. Mostrar erros após interação/submit, rótulos persistentes, `aria-invalid`, `aria-describedby`, foco acessível, sucesso em `aria-live` e erro relevante em `role=alert`.

**Desativação:** apenas para produto ativo e perfil escritor. Confirmar ação e explicar que o registro permanece no histórico; chamar DELETE, aceitar 204 sem body, recarregar página/estado e mostrar inativo. Não oferecer reativação. Se a página ficar vazia após remover o item, ajustar para página válida anterior ou exibir vazia.

## 5. Evoluções necessárias no backend

1. Precisão integral exige contrato decimal string e migração/versionamento compatível, atualização de OpenAPI e todos os consumidores. Java `BigDecimal`, validações e NUMERIC podem permanecer; adicionar testes Jackson de tipos/round-trip incluindo máximo e escala.
2. `Pageable` não define `size` máximo nem allowlist de sort no controller. Para limitar carga e superfície, avaliar tamanho máximo explícito e ordenações suportadas, retornando 400 previsível para valor inválido. Até então UI envia apenas size 20 e `name,asc`.
3. Se número/volume de categorias crescer, adicionar endpoint/filtros de categoria ativa com pesquisa ou paginação apropriada para seletor. Atual serviço de categorias não filtra.
4. Respostas de conflito usam `detail` textual. Para erros estáveis por campo, considerar códigos Problem Details como `SKU_ALREADY_EXISTS` e `CATEGORY_INACTIVE`; não é requisito para a UI inicial.
5. OpenAPI/configuração da URL local diverge segundo a spec de catálogo (server documentado 8080 e environment web 8082); reconciliar em revisão contratual separada.

Não é necessária evolução de authorities para a tela: controller já restringe leitura/escrita conforme a matriz descrita.

## 6. Plano incremental

1. Confirmar decisão de contrato monetário e plano de compatibilidade/versionamento; não começar formulário que prometa fidelidade integral antes disso.
2. Construir listagem standalone, filtros reais, paginação no servidor, estados e guards; cobrir serviço e componente.
3. Implementar carregamento e seletor de categoria, incluindo preservação da categoria inativa já associada.
4. Implementar cadastro/edição com validações de SKU/nome/categoria/preço e tratamento de 400/401/403/404/409.
5. Implementar desativação lógica com confirmação e resposta 204.
6. Integrar contrato monetário decidido, revisar modelos e OpenAPI/consumidores, executar lint/testes/build já existentes e smoke autenticado com papéis adequados.

## 7. Plano de testes

- **API Angular:** URL, filtros `name`, `sku`, `categoryId`, `active`, omissão de opcionais vazios, combinações, paginação zero-based, `size`, `sort=name,asc`; validar resposta PageResponse e 400/401/403/404/409.
- **Lista:** filtros combinados e reset para página zero; navegação anterior/próxima, totais, vazio, loading, retry, erro e categoria exibida. Confirmar que filtro “todos” omite `active`.
- **Formulário:** GET de edição; POST e PUT com DTO completo; normalização/trim de SKU, required, branco, maxLength 64, nome 160, descrição 2000; SKU duplicado/409; categoria obrigatória, ativa para nova associação e preservada inativa para associação existente.
- **Preço:** 0.00, valores com duas casas e vírgula BR, ausência de arredondamento; rejeitar mais de duas casas, negativo, excesso de dígitos e valor acima de `999999999999.99`; máximo e valor adjacente válido/ inválido conforme escala. Testar ida e volta JSON sem converter por `number` após contrato string; testar que formatação exibida não altera payload.
- **Desativação:** confirmação cancelada não chama serviço; confirmação chama DELETE; resposta 204 vazio aceita; estado é recarregado/inativo; não há ação de reativação.
- **Permissões:** ADMIN/MANAGER leem e escrevem; SALES/STOCK_OPERATOR leem sem controles de escrita; FINANCE e usuário sem role não acessam rota; `/me` indisponível falha fechado; backend retorna 403 em tentativas proibidas. Esconder botão não substitui teste da API.

## 8. Critérios de aceite

1. Leitores autorizados consultam lista e detalhe; perfis sem leitura são barrados por guard e API.
2. Lista combina somente filtros reais e pagina no servidor, preservando totalizadores e estado dos filtros.
3. ADMIN/MANAGER criam/atualizam usando todos e somente os campos de `ProductRequest`; SKU/name refletem a resposta normalizada.
4. Categoria inativa não pode ser nova associação; edição pode manter somente o vínculo inativo corrente.
5. Preços BRL são exibidos corretamente e, após migração contratual, preservam cada centavo até o máximo do NUMERIC(14,2), sem arredondamento silencioso.
6. Desativação é lógica, confirma intenção, trata 204 sem corpo e não oferece exclusão permanente ou reativação.
7. Loading, vazio, erros HTTP e falhas de rede têm resposta acessível, sem apagar valores digitados em falha de gravação.
8. Testes automatizados cobrem paginação/filtros, criação/edição, validações, preço e limite, desativação, 204 e permissões.

## 9. Riscos e decisões pendentes

- **Contrato v1 monetário:** escolher migração versionada string decimal; compatibilidade com consumidores atuais precisa ser inventariada. Enquanto number permanecer, precisão integral no browser não pode ser garantida.
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
- `apps/web/src/app/catalog/categories/category-access.guard.ts`
- `apps/web/src/app/catalog/categories/category-list.component.ts` e `category-form.component.ts`
- `apps/web/src/app/app.routes.ts` e `apps/web/src/app/app.component.ts`
- `apps/api/src/main/java/com/erp/core/catalog/api/ProductController.java`, `ProductRequest.java`, `ProductResponse.java`, `PageResponse.java`
- `apps/api/src/main/java/com/erp/core/catalog/application/ProductService.java`
- `apps/api/src/main/java/com/erp/core/catalog/domain/Product.java`, `ProductRepository.java`
- `apps/api/src/main/java/com/erp/core/platform/security/SecurityConfiguration.java`
- `apps/api/src/main/resources/db/migration/V1__foundation_schema.sql`, `V2__catalog_categories_and_products.sql`, `V3__catalog_nonblank_constraints.sql`
- `apps/web/src/app/catalog/data-access/product-api.service.spec.ts` e testes de categoria; `apps/api/src/test/java/com/erp/core/catalog/CatalogIntegrationTest.java`
