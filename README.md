# ERP Core

Fundação técnica de um ERP modular em monólito. Esta fase entrega estrutura, contratos e infraestrutura inicial; não inclui funcionalidades de negócio nem autenticação de negócio completa.

## Estrutura

- `apps/api`: API Spring Boot, com módulos em `com.erp.core`.
- `apps/web`: SPA Angular organizada por funcionalidades.
- `infra`: Docker Compose e configuração local.
- `docs`: visão, requisitos, domínio, segurança, API, ADRs e backlog.
- `.github/workflows`: integração contínua.

## Pré-requisitos fixados

- Java 21 LTS; Maven 3.9.16 (versão inspecionada localmente).
- Spring Boot 3.5.16; PostgreSQL 17.11; Flyway gerenciado e compatível pelo Spring Boot.
- Angular 22.0.0; Node.js 22.22.3 (linha 22); npm 11.6.2.
- Keycloak 26.7.3.
- Docker Engine 29.8.2 e Compose 5.5.1 no ambiente inspecionado; o arquivo usa a especificação Compose compatível.

Consulte [docs/decisions](docs/decisions) para justificativas. As imagens Docker usam versão fixada por tag. Credenciais Compose são apenas padrões descartáveis de desenvolvimento local; substitua por variáveis locais antes de iniciar. Não use estes valores fora de desenvolvimento.

## Execução local

Na raiz, copie os padrões locais (opcional) para poder ajustá-los sem versionar credenciais:

```powershell
Copy-Item infra/.env.example .env
docker compose -f infra/compose.yaml up -d
```

API (requer um JAR já compilado com Java 21; `scripts/verify.ps1` compila e testa):

```powershell
java -jar apps/api/target/erp-core-api-0.1.0-SNAPSHOT.jar
```

Se a porta 8080 estiver ocupada, defina `$env:SERVER_PORT='8082'` antes de iniciar.

Frontend (requer Node/npm):

```powershell
cd apps/web
npm ci
npm start
```

API: http://localhost:8080/actuator/health (ou `http://localhost:8082/actuator/health` quando `SERVER_PORT=8082`). PostgreSQL local: porta 5433. Keycloak de desenvolvimento: http://localhost:8081. O Compose importa `erp-core` a partir de `infra/keycloak/erp-core-realm.json` quando o realm ainda não existe. O cliente web usa Authorization Code + PKCE S256; a API valida access tokens com issuer e audience `erp-core-api`. A conta bootstrap do Keycloak é apenas de desenvolvimento local e seus valores devem ser trocados no `.env` não versionado.

Para aplicar mudanças ao realm que já foi criado, revise a configuração versionada e aplique as alterações por Admin REST API ou `kcadm.sh`; o import no startup ignora realms existentes para preservar usuários e estado.

## Verificações

Validação integrada no Windows:

```powershell
.\scripts\verify.ps1
```

Execução individual:

```powershell
mvn -f apps/api/pom.xml verify
cd apps/web
npm ci
npm run lint
npm test -- --watch=false
npm run build
```

Estado real da validação desta entrega e limitações do ambiente: [docs/validation.md](docs/validation.md).
