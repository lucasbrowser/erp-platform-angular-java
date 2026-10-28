# Validação da fundação

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
