# SDD — Evolução do contrato monetário

**Status:** API v2 de produtos implementada e camada de dados/modelos Angular migrada para `/api/v2/products`. A v1 permanece numérica e inalterada. Formulários e apresentação monetária Angular ainda não implementados.

## 1. Objetivo e decisão

Preservar valores monetários sem perda entre PostgreSQL, Java, HTTP/JSON e Angular. Adotar uma representação decimal canônica em string no contrato HTTP (por exemplo, `"10.99"`), `BigDecimal` no domínio Java e `NUMERIC(14,2)` no banco. Valores monetários permanecem decimais exatos durante validação, persistência e cálculo. Formatação `pt-BR` é somente apresentação.

**Decisão aplicada: alternativa A, nova versão da API.** O contrato string está em `/api/v2/products`; `/api/v1` e seu tipo numérico permanecem sem mudança. Não há prazo de descontinuação de v1 definido.

## 2. Inventário de impactos encontrados

### 2.1 O que existe hoje

O rastreio integral de `BigDecimal`, `NUMERIC`, `price`, `total`, `amount` e termos relacionados encontrou valores monetários implementados apenas no catálogo de produtos. Não foram encontrados controllers, DTOs, entidades, migrations, serviços, testes ou modelos Angular de pedidos/itens de pedidos, preço unitário ou total de pedidos. Portanto, não há cálculo atual de total de pedido para ajustar; se o módulo for introduzido, deve seguir desde o início as regras deste documento.

| Camada | Local encontrado | Representação/uso atual | Impacto |
|---|---|---|---|
| Request API | `ProductRequest` e `ProductV2Request` | v1 recebe número JSON; v2 recebe string validada por tipo JSON estrito e pattern canônico de duas casas | v1 preservada; v2 converte string diretamente a BigDecimal. |
| Serviço | `ProductService` | Passa `request.price()` diretamente ao domínio em create/update | Preservar valor exato; sem aritmética ou arredondamento encontrados. |
| Domínio | `Product` | Campo/getter `BigDecimal price` | Continua BigDecimal; comparar por valor numérico com `compareTo`, não por `equals`, quando escala puder diferir. |
| Response API | `ProductResponse` e `ProductV2Response` | v1 serializa número; v2 serializa string `toPlainString()` após escala 2 com `RoundingMode.UNNECESSARY` | Não há arredondamento na resposta v2. |
| Banco | `V2__catalog_categories_and_products.sql` | `erp.products.price NUMERIC(14,2)`, `NOT NULL`, `CHECK (price >= 0)` | Precisão/escala adequadas; não requer mudança de schema para alterar representação HTTP. |
| API v2 | `catalog/api/v2/ProductV2Controller`, `ProductV2Request`, `ProductV2Response` | Endpoints equivalentes a produtos v1; request recebe string canônica e converte por `new BigDecimal(String)`, response usa `setScale(2, UNNECESSARY).toPlainString()` | Novo contrato textual sem alterar DTOs ou serialização v1. |
| Testes API | `CatalogIntegrationTest` | Espera `$.price` numérico, constrói JSON numérico; testes de constraint usam `BigDecimal` | Manter expectativas de v1; adicionar testes separados de v2, limites e serialização textual. |
| OpenAPI | `docs/api-contract.yaml` | v1 declara preço numérico; v2 declara string com pattern decimal e limite de 12 dígitos inteiros | Os contratos permanecem separados por versão. |
| Modelo Angular | `catalog.models.ts` | `Product.price` e `ProductWriteRequest.price` são `string` | Transporte tipado compatível com v2; não usar `number` em valor monetário. |
| Serviço Angular | `ProductApiService` | GET/POST/PUT/DELETE usam `/api/v2/products`; request é enviado sem conversão | Contrato v2 explícito; `CategoryApiService` continua em `/api/v1/categories`. |
| Teste Angular | `product-api.service.spec.ts` | Verifica URL v2, filtros/paginação, bodies string, preço máximo, 204 e propagação de erro HTTP | Sem dependência de conversão monetária para number. |
| Interface Angular | Formulário e lista de produtos usam o serviço v2 | Entrada e saída monetária tratadas como strings | Parser textual aceita vírgula/ponto, valida escala e limite, e formatter BRL não converte para number. |
| Pedidos | Nenhum arquivo correspondente encontrado em `apps/api`/`apps/web` | Nenhum modelo/endpoint/cálculo de pedido presente | Escopo futuro; preço unitário e total devem ser string no wire e `BigDecimal`/NUMERIC internamente. |

`docs/specs/products-angular.md` e `docs/specs/catalog-angular.md` já registram a perda potencial em JavaScript number e recomendam string decimal com evolução compatível/versionada. Esta decisão consolida a recomendação também para domínios monetários futuros.

### 2.2 Contrato e persistência atuais

O produto persiste `NUMERIC(14,2)`, equivalente a no máximo 12 dígitos inteiros e duas casas; máximo permitido: `999999999999.99`. `ProductRequest` usa `@Digits(integer=12,fraction=2)` e `@DecimalMin("0.00")`. O campo da entidade e response usa `BigDecimal`. A validação Java protege a entrada antes do serviço; o check PostgreSQL protege não negatividade e o tipo limita precisão/escala. A mudança de formato HTTP não exige migração de coluna.

No contrato atual, `BigDecimal` é serializado por Jackson como número JSON. JavaScript `number` não representa todos os centavos ao longo dessa faixa. Um cliente pode receber um valor já aproximado, formatá-lo ou reenviá-lo perdendo informação; usar biblioteca decimal somente no browser não recupera a informação perdida no token numérico recebido.

## 3. Comparação de compatibilidade

| Alternativa | Consumidores e rotas | DTO/OpenAPI | Segurança | Manutenção e precisão |
|---|---|---|---|---|
| **A. Nova versão com string decimal (recomendada)** | Consumidores v1 continuam funcionando sem mudança; consumidores atualizados optam por rota/contrato v2. Exige manter duas versões temporariamente. | DTOs/schema v2 tipados explicitamente para string (pode usar value object/serializador validado); OpenAPI marca string decimal e regras; v1 permanece numérico. | Reutiliza autenticação, authorities, validações de acesso, CORS e políticas; verificar que todos os paths v2 ficam sob `/api/**` e métodos têm mesmas regras. | Fronteira clara, sem ambiguidade por campo; custo temporário de duplicar adaptação/controller/DTO ou camada compartilhada. Migração e remoção planejadas. |
| **B. Período dual com campos string adicionais** | Uma mesma resposta teria campo antigo numérico e campo novo string; clientes precisam escolher. Requests com ambos podem divergir e exigir precedência/erro. | Campos como `price` e `priceDecimal` duplicam conceito, podem sair inconsistentes; OpenAPI e todos os clientes precisam saber o campo válido para cada fase. Não corrige perda para cliente que continuar usando o antigo. | Rotas e authorities podem permanecer iguais, mas o endpoint expõe simultaneamente contratos incompatíveis e amplia superfície de validação. | Evita trocar o nome/path, mas prolonga ambiguidade, requer sincronizar campos e definir conflito; payload aumenta. Só é aceitável como transição curta, com datas e regras estritas. |
| **C. Alterar diretamente o contrato atual** | Quebra consumidores que desserializam `price` como number, ou cria comportamento inconsistente em parsers permissivos. Mesmo que Jackson aceite número e string para BigDecimal na entrada, respostas alterariam o tipo sem opt-in. | Alteração de schema v1 incompatível com código gerado e consumidores; documentação pode não refletir tipo que cada cliente recebe. | Não altera mecanismo de autenticação, mas clientes podem falhar após upgrade enquanto credenciais continuam válidas; segurança não resolve compatibilidade de tipo. | Menor custo inicial e uma implementação, mas risco alto de quebra silenciosa/rollout parcial e difícil rollback coordenado. Não recomendado. |

A é a escolha aplicada e garante opt-in explícito. Alternativa B não foi usada. Não foi definido prazo de retirada da v1.

## 4. Regras monetárias do contrato novo

1. Wire format é string JSON com ponto decimal e sem agrupador ou símbolo de moeda: `"0.01"`, `"10.99"`, `"999999999999.99"`. Não aceitar vírgula, `R$`, notação exponencial, espaços, separador de milhar, `NaN` ou infinito.
2. Representação canônica v2 exige exatamente duas casas e não permite zeros à esquerda além do zero simples. `"10.90"` é válido; `"10.9"`, `"1.230"` e `"01.00"` são rejeitados. A regra está no pattern DTO/OpenAPI.
3. Valor permitido: `0.00` até `999999999999.99`, inclusivo. Negativos, acima do máximo, string inválida, nula/ausente e mais de duas casas são erro 400 de validação. Não arredondar para fazer o valor caber.
4. Converter a string diretamente para `BigDecimal`; validar `scale`/precisão e faixa antes de persistir. Evitar construtor `BigDecimal(double)`. A coluna existente `NUMERIC(14,2)` permanece a defesa persistente adicional.
5. Comparações monetárias devem usar `BigDecimal.compareTo` (`compareTo(...) == 0`) quando igualdade de valor for desejada; `BigDecimal.equals` considera escala (`1.0` não é `1.00`). Para aritmética, manter BigDecimal até a fronteira e explicitar escala/`RoundingMode` somente quando a regra de domínio autorizar arredondamento. Arredondamento não é mecanismo de validação.
6. Totais de pedido futuros devem calcular preço unitário × quantidade e somar em BigDecimal sem `double`/`float`; definir precisão/escala intermediária, limite e regra de arredondamento fiscal/comercial antes de implementar. Não arredondar silenciosamente cada item ou o total.
7. Angular mantém entrada e DTO monetário como string. Não converter por `Number`, `parseFloat`, `toFixed` nem operações aritméticas JS. Se cálculos client-side forem necessários, usar aritmética decimal/integer minor units com faixa segura e contrato bem definido; servidor continua fonte de verdade.
8. Formatação brasileira ocorre somente na apresentação. Não enviar vírgula, símbolo ou texto formatado à API/banco. Formatter deve preservar o decimal exato, inclusive valores próximos do máximo; `Intl.NumberFormat` recebendo um `number` grande não é suficiente. Criar formatter baseado em decimal exato ou separar inteiro/centavos para apresentação.

## 5. Plano incremental de implementação

### Tarefa 1 — Contrato e DTOs do backend

- Definir o tipo explícito de wire decimal string para v2, request e response, sem depender da coerção permissiva padrão do Jackson.
- Criar parser/validador central que converta string canônica para BigDecimal, valide sintaxe, escala, sinal, precisão e máximo e produza erros 400 estáveis.
- Manter domínio `Product.price` BigDecimal e persistência atual; evitar duplicar validação em cada controller.

### Tarefa 2 — Compatibilidade e versionamento

- Inventariar consumidores (web, scripts, integrações, código gerado e clientes externos) e owners.
- Publicar v2 opt-in com v1 inalterada; garantir versionamento explícito em paths e contratos. Replicar regras de acesso por método e autenticação para v2.
- Definir janela de coexistência, telemetria agregada de uso v1 sem registrar valores/tokens, comunicação e critério/data de retirada. Não remover v1 nesta entrega.

### Tarefa 3 — Testes de serialização e validação

- Testar Jackson de v2: resposta string JSON, request aceita somente formatos decididos, ida/volta exata, campos ausentes/nulos/inválidos e Problem Details.
- Manter testes que afirmam número JSON em v1; novos testes v2 devem afirmar que `price` é string e comparar BigDecimal por valor.
- Testar o máximo e limites de escala com validação DTO e integração PostgreSQL.

### Tarefa 4 — Ajustes em pedidos e cálculos

- **Pré-requisito de descoberta:** módulo de pedidos não existe no checkout auditado. Quando esse módulo for criado ou conectado, inventariar todo preço unitário, quantidade, desconto, imposto, subtotal e total antes de codificar.
- Definir precisão de colunas, DTOs wire string, regras de multiplicação/soma e pontos legais/funcionais de arredondamento. Usar BigDecimal em Java e tipos NUMERIC adequados; nunca introduzir double.
- Para consumidores de pedidos legados que venham a existir, aplicar a mesma versão explícita e testes de cálculo antes de liberar.

### Tarefa 5 — Atualização dos modelos e serviços Angular

- **[Concluído nesta etapa]** Tipar os modelos `Product`/`ProductWriteRequest` com preço string e migrar `ProductApiService` para `/api/v2/products`, sem afetar categorias.
- Manter o controle do formulário como texto decimal, com validação local coerente; serializar string canônica e exibir resposta sem converter a number.
- Implementar formatter `pt-BR` sem perda, separando representação e wire. Não adicionar biblioteca até avaliar se formatter/aritmética interna simples atende; se dependência for necessária, propor e testar separadamente.

**Estado:** modelos, serviço HTTP, formulário e lista foram migrados para v2. Formulário usa controle textual, normaliza para duas casas sem arredondar e formatter exibe BRL por transformação textual.

### Tarefa 6 — Testes de integração

- Cobrir v1 e v2 em paralelo, incluindo autoridade, status, payload, validação, persistência PostgreSQL, round-trip de máximo e leitura/edição no Angular.
- Testar compatibilidade com fixture(s) de consumidor v1 e fluxo v2 end-to-end. Verificar que uma resposta v2 nunca é interpretada como number.
- Para pedidos futuros, adicionar testes de preço unitário e totais antes de expor endpoints.

### Tarefa 7 — OpenAPI e documentação

- Em `docs/api-contract.yaml`, manter a definição v1 numérica e adicionar v2 string decimal para todos os campos monetários; especificar pattern, exemplos, limites, erros e sunset/versionamento.
- Alinhar DTOs, controllers, testes de contrato e URLs documentadas (inclusive divergência localhost 8080/8082 já registrada nas specs de catálogo).
- Atualizar `products-angular.md`, `catalog-angular.md`, documentação de onboarding e exemplos para diferenciar v1/v2 e registrar sequência de migração.

**Estado atualizado:** contrato v2, DTOs, validação, segurança e OpenAPI estão implementados no backend. `ProductApiService` chama `/api/v2/products`; modelos `Product` e `ProductWriteRequest` usam `price: string`; categorias continuam na v1. A UI de produtos foi implementada com lista, filtros, paginação, formulário, seleção de categoria, desativação e formatter BRL exato baseado em strings. Entrada monetária aceita vírgula ou ponto, não aceita arredondamento e gera exatamente duas casas para o payload. Testes e build da UI ainda precisam ser executados no ambiente Windows exigido pela tarefa.

## 6. Critérios de aceite

- **`0.01`:** v2 aceita e retorna string `"0.01"`; banco mantém exatamente 1 centavo.
- **`10.99`:** request → resposta → leitura → atualização conserva exatamente `10.99`; formatador mostra `R$ 10,99` sem alterar payload.
- **`999999999999.99`:** valor máximo é aceito, armazenado e retornado como string idêntica; formatador não converte para number nem perde centavos.
- **Mais de duas casas:** strings como `"1.234"` recebem 400 e não são arredondadas; comportamento de zeros finais segue a política de escala explicitamente escolhida.
- **Negativos:** `"-0.01"` e menores recebem 400; não chegam ao banco.
- **Acima do limite:** `"1000000000000.00"` recebe 400, sem truncamento/overflow/rounding.
- **Strings inválidas:** `"abc"`, `"1,23"`, `"R$ 1.23"`, `"1e2"`, `" 1.00 "`, string vazia, nulo e campo ausente são rejeitados conforme schema e resultam erro 400 estruturado.
- **Precisão e cálculo de pedido:** não há pedidos no código auditado; quando introduzidos, cenários de múltiplos itens demonstram soma/multiplicação exata de centavos e arredondamento somente pela regra aprovada. Nenhum trecho usa float/double para valor monetário.
- **Compatibilidade:** fixtures/clientes v1 continuam lendo e enviando number e recebem o mesmo formato até a descontinuação anunciada; v2 retorna string. Nenhuma rota ou DTO existente muda de tipo sem opt-in.
- **Segurança:** rotas v2 mantêm bearer JWT, authorities e 401/403 equivalentes às operações v1 correspondentes.
- **Contrato/documentação:** testes OpenAPI refletem versões, tipos, limites, exemplos e respostas implementadas; nenhuma divergência entre schema e serialização.

## 7.1 Endpoints implementados

- `GET /api/v2/products` — filtros `name`, `sku`, `categoryId`, `active`, Pageable e resposta paginada.
- `GET /api/v2/products/{id}` — consulta.
- `POST /api/v2/products` — cria, retorna 201 e Location.
- `PUT /api/v2/products/{id}` — substituição completa.
- `DELETE /api/v2/products/{id}` — desativação lógica com 204.

Em request, `price` deve ser string JSON decimal canônica com duas casas. Em response, `price` é string com duas casas. V1 segue number JSON. Não houve mudança no schema PostgreSQL, configuração de segurança ou DTOs da v1.

## 7. Riscos e decisões pendentes

- A política lexical foi definida para v2: exigir exatamente duas casas e rejeitar whitespace/zeros à esquerda.
- A rota escolhida é `/api/v2`; formalizar política geral de versionamento para futuras versões.
- Inventariar consumidores externos e estabelecer janela de suporte de v1 antes de anunciar sunset.
- Definir códigos Problem Details estáveis para falhas monetárias e compatibilidade com o handler atual.
- Documentar política comercial/tributária de arredondamento antes de qualquer cálculo de pedido; não existe regra ou implementação encontrada.
- Avaliar se o frontend precisa de biblioteca decimal para cálculos futuros; a mudança de wire string é necessária mesmo com biblioteca e não deve ser confundida com ela.

## 8. Arquivos analisados

- `docs/specs/products-angular.md`
- `docs/specs/catalog-angular.md`
- `apps/api/src/main/java/com/erp/core/catalog/api/ProductRequest.java`, `ProductResponse.java`, `ProductController.java`
- `apps/api/src/main/java/com/erp/core/catalog/api/v2/ProductV2Request.java`, `ProductV2Response.java`, `ProductV2Controller.java`
- `apps/api/src/main/java/com/erp/core/catalog/application/ProductService.java`
- `apps/api/src/main/java/com/erp/core/catalog/domain/Product.java`, `ProductRepository.java`
- `apps/api/src/main/resources/db/migration/V2__catalog_categories_and_products.sql`
- `apps/api/src/test/java/com/erp/core/catalog/CatalogIntegrationTest.java`
- `apps/api/src/test/java/com/erp/core/platform/contract/OpenApiContractTest.java`
- `docs/api-contract.yaml`, `docs/validation.md`, `docs/business-rules.md`
- `apps/web/src/app/catalog/models/catalog.models.ts`
- `apps/web/src/app/catalog/data-access/product-api.service.ts`, `product-api.service.spec.ts`

Também foram pesquisados nomes de classes/arquivos e campos de pedidos, itens, preço unitário, subtotal e total em `apps/api` e `apps/web`; nenhum módulo de pedidos ou campo monetário adicional foi encontrado.
