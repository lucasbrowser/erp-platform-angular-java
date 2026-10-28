# Modelo de segurança

- Keycloak atua como provedor de identidade OIDC; a API Spring Security valida access tokens JWT por issuer e JWKS.
- A API é resource server: não armazena senhas e não emite tokens.
- `/actuator/health` é público e mínimo. `/api/**` exige bearer token; demais rotas não são expostas.
- Em desenvolvimento, Keycloak usa modo `start-dev`, dados efêmeros e credenciais padrão locais explicitamente descartáveis. Portas publicadas ficam em `127.0.0.1`.
- O realm/client OIDC não são provisionados nesta fase; o issuer local deve existir quando a autenticação for integrada. Criar realm/client, mapear roles/claims, políticas de autorização, logout e fluxo de usuário fica para a próxima fase.
- Produção requer TLS, secrets manager, realm e client dedicados, usuários administrativos separados, rotação de segredos, configuração explícita de CORS e revisão de claims. Nunca reutilizar credenciais locais.
- CORS permite apenas `http://localhost:4200` no perfil local.
- Actuator expõe apenas health e info. Não expor env, configprops, beans ou métricas sem controles adicionais.
