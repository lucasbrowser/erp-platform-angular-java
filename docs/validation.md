# Validação da fundação

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
- `docker compose -f infra/compose.yaml up -d`: PostgreSQL 17.11 ficou healthy em 127.0.0.1:5433; Keycloak 26.7.3 concluiu bootstrap e `GET http://localhost:8081/realms/master/.well-known/openid-configuration` retornou issuer master. O realm `erp-core` segue pendente.
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
- Configurar realm/client OIDC de desenvolvimento e validar emissão/validação JWT em etapa própria.
- Revisar imagem Docker por digest antes de uso em ambientes compartilhados/produção.
