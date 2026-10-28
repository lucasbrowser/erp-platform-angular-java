# Validação da fundação

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
2. Entre com uma conta descartável não privilegiada. O cliente deve trocar o código no token endpoint usando o `code_verifier` em memória. Não copie o código, verifier, access token ou refresh token para arquivos, terminal gravado ou logs.
3. Inspecione o **access token** localmente no próprio cliente OIDC, sem imprimir seu valor. Confira apenas `iss`, `aud`, `exp` e `realm_access.roles`: issuer `http://localhost:8081/realms/erp-core`, audience contendo `erp-core-api`, expiração futura e as roles atribuídas à conta. Não decodifique nem envie o ID token como credencial da API. A API valida assinatura usando o JWKS, issuer, expiração e audience.
4. Com a variável `$accessToken` mantida somente na sessão local pelo cliente OIDC e sem ativar transcript/log de headers, faça a chamada no PowerShell:

   ```powershell
   Invoke-RestMethod -Method Get -Uri 'http://localhost:8082/api/v1/platform/me' -Headers @{ Authorization = "Bearer $accessToken" }
   $accessToken = $null
   ```

   O resultado esperado é um objeto com somente `userId` e `authorities`. Para verificar a ausência de autenticação, repita sem o header e espere HTTP 401. Não há exigência de role em `/me`; um principal autenticado sem roles recebe 200 e lista vazia. A verificação de 403 exige uma operação protegida por role; por enquanto ela é coberta pelo endpoint fixture de teste, sem endpoint de diagnóstico permanente.

### Resultado OIDC real e limitações

- `curl.exe` e PowerShell acessaram `http://localhost:8081/realms/erp-core/.well-known/openid-configuration` durante esta execução: HTTP 200; issuer `http://localhost:8081/realms/erp-core` e endpoints de autorização, token e JWKS estavam presentes. A API foi iniciada localmente na porta 8082; `/api/v1/platform/me` sem bearer token respondeu HTTP 401. O processo foi encerrado depois do smoke.
- Nenhum access token real foi obtido nesta execução e nenhuma chamada autenticada ao endpoint foi feita. Portanto, o fluxo OIDC ponta a ponta e a inclusão efetiva da audience/roles em access tokens reais permanecem **pendentes**. A disponibilidade do navegador/conta de desenvolvimento e a chamada autenticada ainda precisam ser verificadas seguindo o procedimento acima.

### Próxima tarefa recomendada

- Completar a validação manual Authorization Code + PKCE com uma conta SALES não privilegiada: confirmar claims localmente sem revelar token, chamar `/api/v1/platform/me`, registrar somente status HTTP e nomes das claims/roles observadas. Em seguida, estabelecer uma operação protegida por role quando o primeiro recurso funcional for aprovado e validar 403 com token sem a permissão.

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
- A API não tem atualmente uma rota operacional com autorização por role: `/api/**` exige autenticação, mas não existe endpoint funcional com `@PreAuthorize`. Por isso não há como observar 403 com um usuário real sem adicionar um endpoint de diagnóstico/negócio, o que ficou fora do escopo. Os testes automatizados anteriores comprovam 403 com JWT de teste no endpoint de teste da suíte.
- Não foram usados implicit flow, direct access grants, senha de usuário ou credenciais administrativas pela API nesta tentativa. Nenhum token, código ou segredo foi registrado.

### Procedimento manual seguro para concluir

1. Com Keycloak local ativo, entrar no Admin Console usando a credencial local configurada fora do Git; criar um usuário descartável de desenvolvimento e atribuir somente a role `SALES`. Não reutilizar conta privilegiada nem registrar a senha.
2. Usar um cliente OIDC local compatível com Authorization Code + PKCE S256, client ID `erp-core-web` e callback `http://localhost:4200/oidc-callback` (permitida pelo redirect local). Não usar implicit/direct grant nem ferramentas externas para decodificar tokens.
3. Deixar o cliente trocar o código usando seu `code_verifier`. Inspecionar localmente, sem imprimir token: `iss`, `exp`, `aud`, `realm_access.roles` e o campo `token_type`/tipo do resultado. Confirmar issuer esperado, audience `erp-core-api`, role `SALES` e enviar somente o **access token** à API (nunca o ID token).
4. Chamar `GET http://localhost:8082/api/v1/example` com `Authorization: Bearer <access_token>` em cliente local que não registre headers. Um resultado diferente de 401 comprova autenticação; atualmente a rota inexistente retorna 404 depois da autenticação.
5. Para observar 403 real, repetir contra uma futura operação com autorização de role definida (ou fixture de integração local equivalente) e token de usuário sem essa role. Não introduzir endpoint permanente apenas para o smoke.

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
