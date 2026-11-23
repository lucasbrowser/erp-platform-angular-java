# Testes de integração PostgreSQL

Os testes Spring Boot carregam as migrations Flyway e validam o mapeamento Hibernate contra o banco configurado. A configuração de teste exige `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` e `TEST_DATABASE_PASSWORD`; ela não usa `DATABASE_URL` nem o endereço padrão do banco de desenvolvimento.

Antes de executar os testes, disponibilize um PostgreSQL real e configure essas três variáveis para um banco dedicado, isolado e sem dados que precisem ser preservados. Não use produção nem o banco persistente `erp_core` do `infra/compose.yaml`. A execução aplica as migrations pendentes no banco escolhido. O usuário precisa ter permissões para criar o schema/tabelas e consultar o histórico Flyway.

Exemplo de configuração no PowerShell, substituindo os valores por credenciais de um banco de teste já preparado:

```powershell
$env:TEST_DATABASE_URL = "jdbc:postgresql://localhost:5433/erp_core_test"
$env:TEST_DATABASE_USERNAME = "erp_test"
$env:TEST_DATABASE_PASSWORD = "<senha-do-banco-de-teste>"
mvn.cmd clean verify
```

Não execute `mvn.cmd clean verify` até confirmar que o URL aponta para o banco exclusivo de testes. Esta configuração evita fallback acidental, mas não consegue determinar se um URL informado manualmente aponta para um ambiente produtivo.
