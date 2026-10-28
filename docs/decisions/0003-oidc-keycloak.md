# ADR-0003: OIDC com Keycloak e Spring Security

- Status: Aceita
- Data: 2026-10-28

## Contexto

O ERP precisa centralizar identidade sem armazenar senhas próprias. Clientes OIDC e a API precisam distinguir tokens destinados ao browser daqueles destinados à API e restringir acesso por perfil no servidor.

## Decisão

- Keycloak 26.7.3 é o provedor OIDC e Spring Security OAuth2 Resource Server protege a API.
- A API usa discovery/JWKS do issuer configurado e valida assinatura, issuer, `exp`/`nbf` e audiência `erp-core-api`. O token aceito é access token; ID token não autentica chamadas à API.
- O browser usa client público `erp-core-web`, Authorization Code + PKCE S256. Implicit Flow, password/direct grants e service accounts não são necessários e ficam desativados.
- `erp-core-api` é uma audiência/resource server bearer-only, sem segredo. Mapper do cliente web adiciona essa audiência ao access token, nunca ao ID token.
- Roles do realm ADMIN, MANAGER, SALES, STOCK_OPERATOR e FINANCE viram authorities `ROLE_*`; roles desconhecidas são ignoradas. A API nega por padrão. `/actuator/health` é a única rota pública; permissões por operação devem ser impostas no servidor.
- `infra/keycloak/erp-core-realm.json` é importado no primeiro boot local. O import do Keycloak não altera realm preexistente; atualizações subsequentes serão aplicadas por script idempotente via Admin REST/kcadm ou por procedimento controlado para o ambiente alvo.

## Consequências

Nenhuma senha de usuário é guardada pelo ERP e a API não confia em token apenas por sua validade criptográfica: issuer e audiência também são obrigatórios. A gestão de perfis deve respeitar a matriz de menor privilégio documentada e ser aplicada endpoint/método por endpoint/método. A configuração local `start-dev`, redirect e hostname HTTP são exclusivos de desenvolvimento.

## Validação e limites

Testes automatizados usam tokens JWT assinados por chave efêmera de teste e exercitam 401/403, issuer, audiência e roles. Isso valida o resource server e seu mapeamento, mas não prova emissão ou fluxo de login do realm real. A prova de ponta a ponta exige obter um access token via Authorization Code + PKCE do realm local e chamar a API com ele; não usar ID token.
