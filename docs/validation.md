# Validação da fundação

## Etapa 3.2.1 — integração Angular com Keycloak OIDC (2026-10-28)

- Configurada a SPA com o adaptador oficial `keycloak-js` 26.2.5 e o cliente público `erp-core-web` do realm `erp-core`; não há client secret. A iniciação usa `check-sso`, Authorization Code (padrão do adaptador), PKCE S256 e callback `/auth/callback`. O arquivo silencioso em `apps/web/public/silent-check-sso.html` dá suporte ao `check-sso` em iframe.
- Ambiente de desenvolvimento aponta para Keycloak `http://localhost:8081` e API `http://localhost:8080/api`. O build production substitui o arquivo de ambiente por valores vazios para não embutir URLs locais; valores do deployment precisam ser fornecidos antes de publicar.
- Rotas `/home` protegida, callback, início e acesso indisponível foram criadas. Return path permite apenas caminho interno da mesma origem. Logout usa o adapter. A tela inicial autenticada consulta `/api/v1/platform/me` e mostra os authorities calculados pela API, sem usar claims de token como autorização no cliente.
- Interceptor envia bearer somente a requisições na origem e caminho base da API configurada, e executa `updateToken(30)` antes da requisição. Não inclui token na discovery, endpoints do Keycloak ou outros hosts. Erros HTTP são propagados sem redirecionamento automático; telas consumidoras devem distinguir 401 e 403.
- Testes adicionados para o shell Angular, configuração OIDC com mock, carregamento do endpoint `/me`, resposta 401/403 e interceptor (API versus host do Keycloak). Eles usam mocks e não comprovam autenticação real nem claims emitidas. Não foi possível executar lint, testes ou build: `node` e `npm` não estão disponíveis no PATH; `node_modules` existente não inclui `keycloak-js`. O comando Compose de validação de configuração terminou com código 0, embora o Docker CLI tenha emitido avisos de acesso negado ao config local. Portanto instalação do lock, compilação e testes ficam pendentes em ambiente Node 22.22.3/npm 11.6.2.
- A discovery local respondeu em `http://localhost:8081/realms/erp-core/.well-known/openid-configuration` com issuer `http://localhost:8081/realms/erp-core` e endpoints de autorização/token no mesmo realm. Isso só confirma reachability/discovery; não houve login com usuário legítimo nem chamada autenticada à API nesta tarefa.

### Validação manual OIDC do frontend

1. Inicie os serviços locais (`docker compose -f infra/compose.yaml up -d`) e a API conforme a seção de execução do README. Execute `cd apps/web; npm ci; npm start` em Node 22.22.3/npm 11.6.2.
2. Acesse `http://localhost:4200`, clique em Entrar e autentique uma conta de desenvolvimento legítima já provisionada no realm. O cliente é `erp-core-web`; callback local `/auth/callback`; o adapter envia PKCE S256 e troca o código pelo access token. Não copie o token para logs, arquivos ou chamados.
3. Para inspecionar claims sem imprimir o segredo: na sessão do navegador, use uma ferramenta local que decodifique em memória somente o payload do access token e exiba apenas `iss`, `aud`, `exp` e nomes em `realm_access.roles`; nunca cole o JWT em sites ou console compartilhado. Confirme issuer do realm e `erp-core-api` em `aud`; não use ID token para API.
4. Navegue para `/home`; a própria tela chama `/api/v1/platform/me` e mostra o `userId` e authorities retornados. Na aba Network, confirme status 200 sem copiar nem exportar o header `Authorization`. Um 401 indica sessão ausente/expirada e não inicia um ciclo automático de login; 403 indica falta de permissão.
5. Ainda pendente: teste de login real no browser e requisição API autenticada com access token real. A conta disponível não foi verificada e não foi criada ou alterada nenhuma conta.

## Etapa 3.1 — catálogo backend (2026-10-28)

### Escopo e decisões

- Implementados cadastros de categorias e produtos em /api/v1, sem frontend, estoque ou outros módulos de negócio.
- Categorias e produtos usam UUID e preservam histórico por desativação lógica. GET por ID e listagens incluem inativos; listagem de produtos aceita filtro active.
- Categoria usa nome obrigatório com trim e unicidade sem diferenciar caixa. SKU usa trim e maiúsculas (Locale.ROOT) e é único. Preço é BigDecimal com limite de duas casas e persiste como NUMERIC(14,2), sem valores negativos.
- Criar produto ou mudar sua categoria exige categoria existente e ativa. Produto associado antes da desativação continua consultável e editável enquanto mantém a mesma categoria.
- Matriz aplicada no servidor: ADMIN/MANAGER criam, atualizam e desativam; ADMIN/MANAGER/SALES/STOCK_OPERATOR consultam; FINANCE não consulta catálogo. Essa regra concretiza a matriz já documentada em security-model.md.
- Paginação tem tamanho padrão 20, máximo 100, execução no banco e DTO de resposta estável. Consultas de produto podem filtrar nome, SKU, categoria e estado.

### Persistência e contrato

- V2__catalog_categories_and_products.sql criou tabelas erp.categories e erp.products, FK de produto para categoria, unique index case-insensitive de nome e unique index de SKU, CHECK de normalização/preço, e índices para categoria/estado/nome.
- V3__catalog_nonblank_constraints.sql acrescentou CHECKs de nomes/SKU trimados e não vazios. V1 foi preservada.
- Hibernate está em ddl-auto: validate e open-in-view: false. Escritas estão em serviços transacionais; entidades JPA não são retornadas pela API.
- Atualizado api-contract.yaml com rotas, DTOs, paginação, códigos HTTP e Problem Details. SnakeYAML 2.4 (já disponível transitivamente no classpath Maven) fez parse do documento e confirmou OpenAPI 3.1.0 e 6 rotas. Revisão manual realizada; não há validador semântico OpenAPI instalado/configurado no repositório, então a validação formal de conformidade de schemas/referências permanece pendente.

### Testes e comandos executados

- mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml -Dtest=CatalogIntegrationTest test: BUILD SUCCESS; 8 testes de integração, 0 falhas/erros/skips.
- mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml verify: BUILD SUCCESS; 25 testes, 0 falhas/erros/skips; JAR empacotado.
- Testes de catálogo executaram em PostgreSQL 17.11 real no ambiente local. Flyway validou/aplicou V2 e V3; Hibernate validou o mapeamento; verificações SQL exercitaram unicidade, trim, preço e FK.
- Cobertura HTTP inclui criação, leitura, listagem paginada e filtros, atualização, desativação, validação, 404/409, 401/403, normalização de SKU e comportamento para categoria inativa. Testes de segurança adicionais usam JWTs RSA sintéticos pelo decoder e converter da API para provar leitura SALES, escrita negada a SALES e leitura negada a FINANCE.
- docker compose -f infra/compose.yaml config --quiet: passou; Docker emitiu aviso de acesso negado à configuração global C:\\Users\\lucas\\.docker\\config.json, sem impedir a resolução do Compose.
- git diff --check: passou sem erros; apenas avisos de conversão LF/CRLF do Git.
- Parse YAML do contrato com SnakeYAML 2.4: passou; versão 3.1.0, seis rotas.
- Não foi necessário iniciar ou apagar volumes do PostgreSQL/Keycloak. Nenhum access token real ou credencial foi usado.

### Exemplo de contrato

POST /api/v1/categories (201):

    {"name":"Hardware","description":"Peças"}

POST /api/v1/products (201):

    {"sku":" ab-120 ","name":"Parafuso","description":null,"categoryId":"<uuid-da-categoria>","price":12.50}

O produto recebe SKU AB-120 e retorna categoryId, price, active, createdAt e updatedAt. GET /api/v1/products?page=0&size=20&name=parafuso&active=true retorna PageResponse com content, page, size, totalElements, totalPages, first e last. Corpos inválidos usam application/problem+json (400), item inexistente 404 e conflito 409.

### Pendências e riscos

- Validador formal OpenAPI continua pendente; o arquivo foi revisado manualmente.
- Não existe endpoint de reativação de categoria/produto. Registros desativados seguem disponíveis para consulta; decidir política de reativação com stakeholders antes de expor essa operação.
- A validação OIDC ponta a ponta real permanece pendente conforme registrado abaixo. Os testes do catálogo usam autenticação simulada para HTTP e tokens assinados de teste para integração com a cadeia de segurança; não substituem token real do Keycloak.
- Não foram implementados testes de concorrência para colisões simultâneas de cadastro; as constraints únicas do PostgreSQL garantem a integridade, com DataIntegrityViolation mapeada a 409.

### Próxima tarefa recomendada

- Revisar a matriz e a política de reativação com stakeholders, validar formalmente api-contract.yaml em CI e completar o smoke OIDC real pendente antes de iniciar a interface Angular do catálogo.

## Endpoint de identidade atual — 2026-10-28

### Contrato implementado

- `GET /api/v1/platform/me` exige autenticação bearer. Retorna exatamente `userId` e `authorities`.
- `userId` vem exclusivamente do `sub` do JWT já validado pelo Spring Security. `authorities` contém as authorities `ROLE_*` resultantes do converter de produção, ordenadas; apenas roles ERP conhecidas são convertidas. Não são retornados tokens, claims adicionais ou credenciais.
- A rota não exige role específica: permite que qualquer principal autenticado consulte sua própria identidade. Com token válido sem roles ERP, `authorities` deve ser vazio; ausência/invalidez de autenticação resulta em 401.
- Nenhuma validação ou configuração de segurança foi afrouxada. Issuer, assinatura, expiração e audience continuam validados pelo decoder de produção.

### Validação automatizada

- `mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml verify`: **BUILD SUCCESS** em 2026-10-28; 15 testes executados, 0 falhas, 0 erros, 0 ignorados. Flyway validou a migração existente no PostgreSQL 17.11 e o JAR foi empacotado.
- `SecurityConfigurationTest` (14 testes): inclui `/me` sem token (401), token malformado (401), resposta exata com somente os dois campos, comparação das authorities com o converter de produção, issuer e audience incorretos rejeitados, além dos testes anteriores de expiração, assinatura/roles e 403 de operação protegida de teste.
- A resposta exata foi comparada como JSON estrutural, assegurando ausência de propriedades extras. Tokens de teste são sintéticos e assinados com chave efêmera; isso não equivale a um token emitido pelo Keycloak.
- `git diff --check`: executado; sem erros de whitespace (Git emitiu apenas avisos de conversão LF/CRLF configurada no working tree).

### Procedimento manual para validar com Keycloak

1. Inicie a stack local com `docker compose -f infra/compose.yaml up -d`. Use apenas contas locais de desenvolvimento cujas credenciais foram provisionadas fora do Git. O cliente é `erp-core-web`, com callback local `http://localhost:4200/oidc-callback`; use um cliente OIDC que implemente Authorization Code com PKCE S256. Não use implicit flow nem direct access grants.
2. Entre somente com uma conta de desenvolvimento legítima já existente. Não crie contas nem altere roles para este smoke. O cliente deve trocar o código no token endpoint usando o `code_verifier` em memória. Não copie o código, verifier, access token ou refresh token para arquivos, terminal gravado ou logs.
3. Inspecione o **access token** localmente no próprio cliente OIDC, sem imprimir seu valor. Confira apenas `iss`, `aud`, `exp` e `realm_access.roles`: issuer `http://localhost:8081/realms/erp-core`, audience contendo `erp-core-api`, expiração futura e as roles atribuídas à conta. Não decodifique nem envie o ID token como credencial da API. A API valida assinatura usando o JWKS, issuer, expiração e audience.
4. Com a variável `$accessToken` mantida somente na sessão local pelo cliente OIDC e sem ativar transcript/log de headers, faça a chamada no PowerShell:

   ```powershell
   Invoke-RestMethod -Method Get -Uri 'http://localhost:8082/api/v1/platform/me' -Headers @{ Authorization = "Bearer $accessToken" }
   $accessToken = $null
   ```

   O resultado esperado é um objeto com somente `userId` e `authorities`. Para verificar a ausência de autenticação, repita sem o header e espere HTTP 401. Não há exigência de role em `/me`; um principal autenticado sem roles recebe 200 e lista vazia. Em seguida, consulte `http://localhost:8082/api/v1/products` com a mesma variável: uma role ADMIN, MANAGER, SALES ou STOCK_OPERATOR deve receber 200; uma conta FINANCE sem role de leitura do catálogo deve receber 403.

### Resultado OIDC real e limitações

- `curl.exe` e PowerShell acessaram `http://localhost:8081/realms/erp-core/.well-known/openid-configuration` durante esta execução: HTTP 200; issuer `http://localhost:8081/realms/erp-core` e endpoints de autorização, token e JWKS estavam presentes. A API foi iniciada localmente na porta 8082; `/api/v1/platform/me` sem bearer token respondeu HTTP 401. O processo foi encerrado depois do smoke.
- Nenhum access token real foi obtido nesta execução e nenhuma chamada autenticada ao endpoint foi feita. Portanto, o fluxo OIDC ponta a ponta e a inclusão efetiva da audience/roles em access tokens reais permanecem **pendentes**. A disponibilidade do navegador/conta de desenvolvimento e a chamada autenticada ainda precisam ser verificadas seguindo o procedimento acima.

### Próxima tarefa recomendada

- Completar a validação manual Authorization Code + PKCE com uma conta de leitura do catálogo já existente: confirmar claims localmente sem revelar token, chamar `/api/v1/platform/me` e `GET /api/v1/products`, registrando apenas status HTTP. Se houver conta FINANCE existente sem role de catálogo, confirmar 403 nessa leitura.

## Fase 1.2 — especificação consolidada e OIDC (2026-10-28)

### Estado e escopo

- `git status --short`, `git diff` e `git diff --cached` foram inspecionados antes das alterações; não havia modificações pendentes no início desta tarefa.
- A versão já registrada da fundação foi consultada. Frontend, lint e build não foram repetidos; não houve alteração do frontend ou de dependências.
- A stack Compose já estava disponível. O serviço Keycloak foi recriado isoladamente para montar o realm declarativo; o container PostgreSQL/volume existentes foram preservados.

### Comandos e resultados desta fase

- `Get-Content -Raw -Encoding UTF8 infra/keycloak/erp-core-realm.json | ConvertFrom-Json`: passou; realm `erp-core`, 2 clientes e 5 roles de realm.
- `docker compose -f infra/compose.yaml config --quiet`: passou. O Docker emitiu aviso de acesso negado ao arquivo global `C:\Users\lucas\.docker\config.json`, já observado na etapa anterior.
- `docker compose -f infra/compose.yaml up -d --force-recreate keycloak`: passou; somente o container Keycloak foi recriado e o PostgreSQL permaneceu em execução.
- `docker compose -f infra/compose.yaml logs --tail 80 keycloak`: confirmou `Realm 'erp-core' imported` e `Import finished successfully` no Keycloak 26.7.3.
- Requisição a `http://localhost:8081/realms/erp-core/.well-known/openid-configuration`: passou; issuer anunciado igual ao issuer configurado na API, com authorization/token endpoints e URI JWKS no realm esperado.
- Requisição ao JWKS anunciado: passou; 2 chaves de assinatura foram retornadas.
- `java -jar apps/api/target/erp-core-api-0.1.0-SNAPSHOT.jar --server.port=8082`: iniciou com Spring Security atualizado; smoke HTTP confirmou health `200` e `/api/v1/example` sem bearer token `401`. O processo foi encerrado após o smoke.
- `mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml verify`: passou; BUILD SUCCESS, 10 testes executados, 0 falhas/erros/skips, jar empacotado e Flyway validou a migração V1 no PostgreSQL 17.11.

### Cobertura e limites da evidência

- A suíte valida o decoder de produção contra um JWKS HTTP efêmero e JWTs RSA assinados no teste: ausência de token, token malformado, issuer incorreto, audiência incorreta, token expirado, assinatura válida com permissão, usuário sem a permissão requerida (403) e conversão das roles conhecidas. Nenhuma credencial real foi usada; o JWKS de teste usa chave efêmera.
- A importação, discovery e leitura do JWKS reais do Keycloak local foram verificadas. **Não foi obtido nem enviado à API um access token emitido pelo realm via Authorization Code + PKCE**; portanto a jornada OIDC ponta a ponta, as claims/audiência emitidas pelo mapper e a autorização com usuários/roles reais seguem pendentes.
- A discovery OIDC informa capacidades gerais do realm; isso não demonstra que o cliente `erp-core-web` habilite tais fluxos. O JSON versionado desliga implicit, direct grants e service accounts para os dois clientes.
- Os testes não levantam Keycloak no Maven/CI; são reproduzíveis sem o provedor e não devem ser reportados como teste de integração de tokens reais.

### Próximos passos pendentes

- Obter e validar um access token real pelo fluxo de autorização com PKCE em desenvolvimento e confirmar a audiência `erp-core-api` no access token (nunca usar ID token na API).
- Conferir a configuração efetiva do cliente importado no Admin Console após o primeiro import e atribuir roles a usuários de desenvolvimento fora do arquivo versionado, sem incluir senhas no Git.
- Refinar a matriz de permissões com stakeholders antes de implementar endpoints de negócio. Não implementar módulos funcionais como parte desta fase.

## Validação OIDC ponta a ponta — tentativa de 2026-10-28

### Configuração ativa verificada sem credenciais

- `git status --short` e `git diff --stat`: árvore limpa no início desta tentativa; nenhum arquivo versionado foi alterado antes da inspeção.
- Realm exportado: `erp-core`; cliente web público `erp-core-web`; redirect `http://localhost:4200/*`; web origin `http://localhost:4200`; PKCE S256; flows implícitos, direct grants e service accounts desativados. O mapper `oidc-audience-mapper` configura `erp-core-api` em access tokens e desliga a claim de ID token. O realm declara os cinco perfis iniciais.
- A configuração foi importada no Keycloak 26.7.3 segundo o log registrado na Fase 1.2. Discovery atual respondeu com issuer `http://localhost:8081/realms/erp-core`, endpoint de autorização/token e JWKS esperado; o JWKS respondeu com duas chaves.
- Pedido ao endpoint de autorização com `client_id=erp-core-web`, redirect local e PKCE S256 chegou à página de autenticação (HTTP 200). O mesmo pedido sem parâmetros PKCE redirecionou à callback com erro OAuth; redirect URI fora do host permitido foi rejeitado (HTTP 400).
- POST inválido ao token endpoint com origem `http://localhost:4200` respondeu HTTP 400 e incluiu apenas essa origem em `Access-Control-Allow-Origin`; com `http://localhost:4201`, respondeu HTTP 403 sem permitir CORS. Nenhum código ou token foi usado nesses pedidos.
- API atual iniciada na porta 8082; `GET /api/v1/example` sem autenticação respondeu HTTP 401. O JAR foi encerrado após o teste.

### Limites desta tentativa

- O controle de navegador CUA falhou ao inicializar (kernel encerrou antes de disponibilizar navegador). Nenhuma conta de teste foi provisionada nesta tentativa, e nenhum Authorization Code foi trocado por token.
- Sem login interativo, não foi inspecionado nenhum access token real: a audience `erp-core-api`, `realm_access.roles` e a distinção access token/ID token ainda não foram confirmadas em emissão efetiva. Também não foi feita chamada autenticada à API.
- Na data daquela tentativa, a rota funcional autorizada por role ainda não estava disponível. A Etapa 3.1 implementou autorização no catálogo; nesta tarefa o smoke real pode usar `GET /api/v1/products` para observar 200 ou 403, conforme a role da conta já existente.
- Não foram usados implicit flow, direct access grants, senha de usuário ou credenciais administrativas pela API nesta tentativa. Nenhum token, código ou segredo foi registrado.

### Procedimento manual seguro para concluir

1. Com Keycloak local ativo, usar somente uma conta de desenvolvimento legítima já existente e com perfil apropriado. Esta validação não cria usuários nem altera atribuições de roles; se não houver conta SALES/STOCK_OPERATOR existente, deixar as chamadas autenticadas pendentes.
2. Usar um cliente OIDC local compatível com Authorization Code + PKCE S256, client ID `erp-core-web` e callback `http://localhost:4200/oidc-callback` (permitida pelo redirect local). Não usar implicit/direct grant nem ferramentas externas para decodificar tokens.
3. Deixar o cliente trocar o código usando seu `code_verifier`. Inspecionar localmente, sem imprimir token: `iss`, `exp`, `aud`, `realm_access.roles` e o campo `token_type`/tipo do resultado. Confirmar issuer esperado, audience `erp-core-api`, role `SALES` e enviar somente o **access token** à API (nunca o ID token).
4. Chamar `GET http://localhost:8082/api/v1/platform/me` com o access token mantido em memória; esperar 200 e somente `userId`/`authorities` na resposta. Em seguida, chamar `GET http://localhost:8082/api/v1/products`: esperar 200 para ADMIN, MANAGER, SALES ou STOCK_OPERATOR, e 403 para FINANCE sem uma role de leitura do catálogo.
5. Repetir `GET /api/v1/products` sem o header bearer e esperar 401. Não introduzir endpoints de diagnóstico nem alterar roles para obter os resultados.

Até cumprir esses passos, a validação OIDC ponta a ponta continua **pendente**; a configuração declarativa e os endpoints públicos do provedor foram verificados, mas não se afirma integração autenticada real.

## Ambiente inspecionado em 2026-10-28

- Windows 10, PowerShell.
- Java 21.0.10 LTS e Maven 3.9.16 encontrados.
- Docker CLI 29.8.2 e Compose 5.5.1 encontrados; leitura do arquivo global `C:\Users\lucas\.docker\config.json` retornou acesso negado no sandbox. O daemon foi acessado com permissão elevada para validar a stack.
- `node`, `npm`, `corepack`, `pnpm`, `yarn`, Angular CLI e `psql` não encontrados no PATH.
- Repositório inicialmente continha somente README e .gitignore; árvore Git sem alterações pendentes.

## Comandos executados

- `git ls-files`, listagem de arquivos, leitura do README/.gitignore e `git status --short`: concluíram; apenas dois arquivos existiam e não havia alterações pendentes.
- `java -version`: concluiu; Java 21.0.10.
- `mvn -version`: concluiu; Maven 3.9.16.
- `node --version` e `npm --version`: não executaram; comandos não encontrados.
- `docker --version`: concluiu; Docker 29.8.2.
- `docker compose -f infra/compose.yaml config`: passou e resolveu as portas em `127.0.0.1`; aviso de acesso negado ao arquivo global de configuração Docker permaneceu.

## Resultado desta sessão

- `mvn -s <settings temporário> -B -ntp -f apps/api/pom.xml verify`: passou; BUILD SUCCESS, 4 testes executados, 0 falhas/erros. Compilação, empacotamento e teste de contexto passaram. O cache Maven foi colocado em `%TEMP%` devido ao caminho global sem acesso e a falhas intermitentes de leitura no cache dentro de OneDrive.
- Flyway validou e aplicou `V1__foundation_schema.sql` em PostgreSQL 17.11 sem aviso de versão não suportada.
- `java -jar apps/api/target/erp-core-api-0.1.0-SNAPSHOT.jar --server.port=8082`: API iniciou; `GET /actuator/health` retornou `{"status":"UP"}`. A porta 8080 já estava ocupada por um container preexistente, então o smoke usou 8082.
- `docker compose -f infra/compose.yaml up -d`: PostgreSQL 17.11 ficou healthy em 127.0.0.1:5433; Keycloak 26.7.3 concluiu bootstrap e `GET http://localhost:8081/realms/master/.well-known/openid-configuration` retornou issuer master. Naquele registro inicial o realm `erp-core` ainda não estava criado; ver resultados da Fase 1.2 acima.
- O primeiro pull usou uma tag PostgreSQL inexistente e a porta 5432 já estava ocupada; ambas as condições foram corrigidas sem interferir no container preexistente.
- Node 22.22.3 e npm 11.6.2 foram executados em container temporário porque não estavam no PATH do Windows. `npm install` gerou `apps/web/package-lock.json` (546 pacotes).
- Na cópia temporária, `npm ci --no-audit --no-fund`: passou (549 pacotes instalados); `npm run lint`: passou; Prettier: todos os arquivos passaram; `npm test -- --watch=false`: passou (1 arquivo, 1 teste); `npm run build`: passou, saída em `dist/erp-core-web`.
- A primeira execução Angular revelou `baseUrl` depreciado no TypeScript 6; a opção foi removida e a suíte completa foi repetida com sucesso.
- `ng serve` em container temporário na porta 4200: HTTP 200 e elemento `<erp-root>` confirmado; processo de smoke encerrado.
- A stack Compose PostgreSQL/Keycloak foi deixada em execução. O processo de smoke da API na porta 8082 e o servidor frontend temporário foram encerrados.

## Limitações e próximos passos

- Node/npm não foram instalados no Windows; os comandos frontend foram validados em container com versões fixadas.
- O workflow está configurado, mas não foi disparado em GitHub nesta sessão; os comandos equivalentes de backend, frontend e migração passaram localmente.
- O smoke de PostgreSQL, Keycloak, API e Flyway foi executado conforme acima; a stack Compose permaneceu ativa.
- Validar um access token real emitido por `erp-core` via Authorization Code + PKCE, incluindo audience `erp-core-api`; a Fase 1.2 ainda não executou essa jornada ponta a ponta.
- Conferir configurações efetivas de client/mapper no Admin Console e atribuir roles a usuários locais sem versionar senhas.
- Obter revisão de stakeholders para a matriz de permissões antes de implementar operações.
- Revisar imagem Docker por digest antes de uso em ambientes compartilhados/produção.

## Etapa 3.1.1 — estabilização do catálogo (2026-10-28)

### Inspeção e contrato

- `git status --short` e `git diff` foram inspecionados antes das alterações; a árvore estava limpa. Migrations e volumes existentes foram preservados.
- Adicionado `swagger-parser-v3` 2.1.48 com escopo `test`, versão fixada. O projeto não tinha ferramenta semântica OpenAPI; Swagger Parser declara suporte à especificação 3.1 desde a linha 2.1.0. A validação está em `OpenApiContractTest`, então roda no `mvn verify` e rejeita mensagens de validação, versão divergente, rotas ou métodos diferentes dos controllers.
- O teste semântico passou: OpenAPI 3.1.0, sete caminhos, métodos GET/POST/PUT/DELETE esperados e referências resolvidas, sem mensagens de validação. Inclui `/actuator/info`, exposto pelo Actuator mas negado por padrão (401 sem token e 403 com token válido). A revisão comparou filtros `name`, `sku`, `categoryId`, `active`, paginação `page`/`size`, ordenação `sort`, UUID, nulabilidade, precisão e limite decimal, Problem Details, Bearer, 401/403 e respostas 201/204/400/404/409 com controllers, DTOs, validação e handler de erros.
- Divergências corrigidas em `api-contract.yaml`: schemas de request não mais proíbem propriedades extras, pois o desserializador atual ignora propriedades desconhecidas; respostas de Categoria/Produto declaram `description` como obrigatório e nullable, tal como os records serializados; requisitos `@NotBlank` e limite superior de `BigDecimal` foram representados; resposta Problem Details declara seus campos garantidos; e a ordenação padrão `name,asc` ficou documentada. Não houve mudança de comportamento da API.
- Atualizações de Categoria e Produto não recebem nem escrevem `active`. Testes de integração agora enviam `active:true` depois da desativação e verificam que o estado continua falso. Reativação permanece sem operação explícita e pendente de decisão funcional; desativação lógica/histórico foram preservados.

### Validações executadas

- `mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml verify`: **BUILD SUCCESS**, 27 testes, 0 falhas, 0 erros, 0 ignorados; Flyway validou três migrations e o PostgreSQL 17.11 já estava na versão 3. A primeira tentativa em sandbox não alcançou Maven Central; a execução autorizada com acesso à rede baixou o validador e concluiu o build.
- `docker compose -f infra/compose.yaml config --quiet`: **passou**; Docker reportou acesso negado à configuração global `C:\Users\lucas\.docker\config.json`, mas a configuração Compose foi validada.
- `git diff --check`: **passou**, sem erros; Git emitiu somente avisos de conversão LF/CRLF.
- Discovery `GET http://localhost:8081/realms/erp-core/.well-known/openid-configuration`: HTTP 200 durante a inspeção; issuer `http://localhost:8081/realms/erp-core`, authorization endpoint, token endpoint e JWKS anunciados no realm esperado. A API iniciada localmente em `localhost:8082` respondeu `GET /actuator/health` 200, `GET /api/v1/platform/me` sem bearer 401 e `GET /api/v1/products` sem bearer 401; o processo da API foi encerrado após as sondagens. Nenhum serviço persistente foi parado e nenhum volume foi apagado.

### Smoke OIDC real: pendente

- Realm declarativo conferido: `erp-core`; cliente `erp-core-web` público; Authorization Code habilitado; PKCE `S256`; redirect local `http://localhost:4200/*`; web origin `http://localhost:4200`; implicit, direct grants e service accounts desabilitados. O mapper inclui audience `erp-core-api` no access token e não no ID token. As roles de realm são ADMIN, MANAGER, SALES, STOCK_OPERATOR e FINANCE. O export não contém usuários. O Admin Console/Admin REST não foi consultado nesta tarefa; portanto, as opções efetivas do realm já importado não foram revalidadas nesta execução.
- Não foi obtido token real nem realizada chamada autenticada. A discovery comprova que o provedor responde, mas não comprova emissão efetiva de audience/roles. A conta local legítima disponível e a integração OIDC do frontend não foram verificadas; portanto `/me` e leitura autenticada (200) e leitura sem role (403) permanecem pendentes. O caso sem bearer (401) foi observado por HTTP real nesta execução. Os testes automatizados também cobrem 401/403 com autenticação simulada.
- Para execução humana: (1) iniciar PostgreSQL/Keycloak com `docker compose -f infra/compose.yaml up -d` e a API com `mvn "-Dmaven.repo.local=C:/Users/lucas/AppData/Local/Temp/erp-core-maven-repository" -B -ntp -f apps/api/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"`; (2) conferir discovery e issuer acima; (3) usar um cliente OIDC local que implemente Authorization Code + PKCE S256, client ID `erp-core-web`, callback `http://localhost:4200/oidc-callback` e somente conta legítima já existente; o pedido de autorização deve incluir `response_type=code`, `scope=openid`, `state`, `code_challenge` e `code_challenge_method=S256`; a troca no token endpoint usa `grant_type=authorization_code`, o mesmo `redirect_uri`, o `code` recebido e seu `code_verifier`, sem client secret (cliente público); não criar usuário, mudar role ou usar conta privilegiada para simular perfil; (4) manter tokens somente em memória e conferir localmente no **access token** `iss`, `aud`, `exp` futuro e `realm_access.roles`, sem imprimir/salvar token, código, verifier, cookie ou segredo; confirmar issuer e audience `erp-core-api`, e enviar esse access token (nunca ID token) à API, cuja validação criptográfica permanece ativa; (5) sem registrar headers, chamar `GET /api/v1/platform/me` e `GET /api/v1/products`; esperar 200 para ADMIN/MANAGER/SALES/STOCK_OPERATOR e 403 para FINANCE sem role de leitura; repetir a leitura de catálogo sem bearer e esperar 401. Registrar apenas status HTTP e nomes de claims/roles. Se não houver contas locais com esses perfis, manter os casos correspondentes pendentes.

### Pendências e próxima tarefa

- Pendente: executar o fluxo real com conta SALES ou STOCK_OPERATOR existente e obter 200 em `/me` e `/products`; obter 403 com conta FINANCE existente, caso disponível; e confirmar 401 em chamada HTTP sem bearer na API iniciada. Não há credenciais no export e nenhuma conta foi criada.
- Pendente: decisão funcional aprovada sobre reativação. Até lá, atualização não pode alterar `active` e não deve ser criada operação de reativação.
- A interface Angular de catálogo deve aguardar a validação real do access token/roles e a escolha da integração OIDC do frontend. A leitura automatizada do contrato agora é parte do `verify`.
