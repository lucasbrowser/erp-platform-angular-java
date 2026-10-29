# Modelo de segurança

## Identidade e tokens

- Keycloak 26.7.3 emite tokens OIDC; a API Spring Security é OAuth 2.0 Resource Server e não armazena senhas, não autentica credenciais próprias e não emite tokens.
- O browser usa o cliente público `erp-core-web`, Authorization Code Flow com PKCE S256. Implicit Flow, Direct Access Grants e service accounts ficam desativados.
- A SPA usa o adaptador oficial `keycloak-js` 26.2.5. O callback é `/auth/callback`, a verificação silenciosa usa `/silent-check-sso.html` e os tokens permanecem somente na memória do adaptador; `sessionStorage` guarda apenas a rota interna de retorno durante o redirecionamento.
- O interceptor do frontend só anexa bearer a URLs com a mesma origem e o caminho base configurado da API (`http://localhost:8082/api` no desenvolvimento). Ele atualiza o access token antes da chamada e deixa requisições a outros hosts, inclusive Keycloak, sem token. Um 401 não inicia login automático; 403 deve ser apresentado pela tela que solicitou a operação.
- Rotas privadas usam guard apenas para navegação. A autorização real continua obrigatória na API Spring Security. A tela de início é apenas uma página autenticada de fundação e não implementa catálogo.
- A página autenticada consulta `GET /api/v1/platform/me` e exibe apenas `userId` e authorities devolvidas pela API; respostas 401, 403 e demais falhas são tratadas separadamente. Ela não deriva a identidade do token no cliente.
- A API aceita somente **access tokens** cujo issuer seja `http://localhost:8081/realms/erp-core`, cuja assinatura seja verificada com o JWKS desse issuer e cuja audiência contenha `erp-core-api`. ID tokens não são credenciais de API.
- Validação de assinatura, `iss`, `exp`/`nbf` e audiência é obrigatória. Falha ou ausência de bearer token resulta em 401; usuário autenticado sem autoridade exigida resulta em 403.
- Roles reconhecidas vêm de `realm_access.roles` e são convertidas para authorities `ROLE_*`. Claims desconhecidas não concedem acesso. O frontend pode ocultar ações, mas toda autorização deve ser aplicada no servidor, por operação.

## Perfis e operações previstas

Perfis são roles do realm. A matriz inicial define o menor privilégio por operação. Para o módulo de catálogo implementado nesta etapa, a API aplica diretamente no servidor a leitura abaixo e restringe criação, atualização e desativação a ADMIN/MANAGER.

| Operação | ADMIN | MANAGER | SALES | STOCK_OPERATOR | FINANCE |
|---|---:|---:|---:|---:|---:|
| Administrar usuários e atribuir perfis | Sim | Não | Não | Não | Não |
| Consultar cadastros de produtos | Sim | Sim | Sim | Sim | Não |
| Criar/editar cadastros de produtos | Sim | Sim | Não | Não | Não |
| Desativar categorias e produtos | Sim | Sim | Não | Não | Não |
| Consultar estoque | Sim | Sim | Sim | Sim | Não |
| Registrar movimentação/ajuste de estoque | Sim | Não | Não | Sim | Não |
| Criar e consultar vendas | Sim | Sim | Sim | Não | Não |
| Confirmar vendas (sujeito a estoque suficiente) | Sim | Sim | Sim | Não | Não |
| Consultar contas a pagar/receber | Sim | Sim | Não | Não | Sim |
| Registrar baixa manual de contas | Sim | Não | Não | Não | Sim |
| Consultar relatórios operacionais | Sim | Sim | Conforme módulo | Conforme módulo | Conforme módulo |

ADMIN representa administração técnica do ERP, não autorização para ignorar validações de domínio. A matriz ainda requer validação do negócio para módulos além do catálogo; acesso a dados pessoais e alterações privilegiadas devem ser auditados.

No catálogo, categorias seguem as mesmas permissões de leitura e escrita dos produtos. As rotas de leitura exigem ROLE_ADMIN, ROLE_MANAGER, ROLE_SALES ou ROLE_STOCK_OPERATOR; operações de escrita exigem ROLE_ADMIN ou ROLE_MANAGER. FINANCE não recebe acesso ao catálogo. Essa política aplica-se na API com @PreAuthorize; ocultar ações na interface não substitui a autorização do servidor.

Categoria e produto são desativados logicamente e mantêm seu histórico. Os DTOs de atualização não incluem `active`, e as operações de atualização preservam o estado atual. Não há operação explícita de reativação; a política funcional para reativar registros desativados permanece pendente de decisão. Até essa decisão, não se deve inferir reativação a partir de atualizações comuns.

## Superfície pública e ambiente local

- Só `/actuator/health` e seus sub-recursos de health são públicos. Endpoints `/api/**` exigem bearer token; demais caminhos são negados por padrão.
- Actuator expõe somente health e info; detalhes do health não são públicos. `/actuator/info` permanece inacessível, resultando em 401 sem token e 403 para principal autenticado por causa do deny-by-default.
- CORS no backend restringe as origens locais `http://localhost:4200` e `http://127.0.0.1:4200`, permite os métodos de API usados pelo frontend e os headers `Authorization` e `Content-Type`; não permite credenciais de browser. Origins adicionais exigem inclusão explícita.
- Compose publica PostgreSQL e Keycloak somente em loopback. O realm importado restringe redirect URI e web origin ao frontend local, desativa cadastro público e não contém usuários ou senhas.
- Credenciais bootstrap de desenvolvimento são apenas placeholders locais via `.env.example`; substituir por valores descartáveis em `.env` não versionado. Nunca reutilizar em ambiente compartilhado. Logs não devem imprimir tokens, senhas ou secrets.

## Fora do escopo / operação segura

MVP é para uma única empresa. Multi-tenancy, emissão fiscal e integrações bancárias automáticas ficam fora do escopo. Logout, recuperação de conta e ciclo de vida de usuário serão tratados pela jornada padrão do Keycloak quando necessários; não há autenticação própria.

Produção requer TLS, secret manager, realm/client próprios, redirect URIs exatas, CORS explícito, configuração de issuer/audience, gestão de chaves e revisão de claims. A configuração `start-dev`, o hostname local e placeholders Compose não são para produção.

## Provisionamento e evolução

`infra/keycloak/erp-core-realm.json` é a fonte declarativa para bootstrap. `start-dev --import-realm` importa o realm quando ele ainda não existe; o import de inicialização ignora realm existente. Para mudanças posteriores, atualizar o JSON, abrir o Admin Console no realm `erp-core`, selecionar **Realm settings → Action → Partial import**, carregar o arquivo versionado e escolher explicitamente os recursos e a política de sobrescrita; revisar o resultado contra o JSON. Para automação, traduzir as mesmas mudanças para comandos `kcadm.sh`/Admin REST idempotentes versionados. Em ambiente descartável pode-se excluir o realm e reiniciar para bootstrap limpo; isso apaga usuários e configuração, então não automatizar em dados compartilhados.

Para testes locais com tokens reais, primeiro iniciar a stack e autenticar por Authorization Code/PKCE no Keycloak. Testes automatizados desta etapa assinam JWT de teste com chave efêmera e verificam a cadeia de filtros, assinatura, issuer, expiração, audience e autorização; eles não substituem um smoke de token emitido pelo realm real.
