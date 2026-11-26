# Padrões de código

As convenções abaixo consolidam padrões encontrados no projeto. Quando um módulo ainda não tem implementação, as orientações são regras para novas contribuições compatíveis com a arquitetura, não afirmações de código já existente.

## Java e Spring Boot

- Use Java 21 e pacotes `com.erp.core.<modulo>`. No backend existente, módulos separam `api`, `application` e `domain` conforme a necessidade; não crie camadas vazias ou abstrações genéricas sem motivo.
- Use nomes Java convencionais, tipos explícitos para IDs (`UUID`), datas (`Instant`), dinheiro (`BigDecimal` no domínio/persistência) e documentos identificadores como `String` quando zeros ou letras forem significativos.
- Prefira objetos imutáveis/records para contratos quando apropriado. Requests e responses REST não são entidades JPA; controllers convertem entre contrato e aplicação/domínio.
- Use injeção por construtor. Controllers mapeiam HTTP e validam limites do contrato; serviços de aplicação coordenam casos de uso e transações; domínio protege invariantes; repositories encapsulam persistência.
- Valide entradas com Jakarta Bean Validation na fronteira HTTP quando aplicável e mantenha invariantes de domínio protegidas também no backend. Normalização deve ter uma responsabilidade única.
- Mapeie falhas conhecidas para Problem Details no handler global. Não retornar stack traces, SQL, credenciais, documentos completos ou mensagens internas sensíveis.
- Aplique autorização no servidor, explicitamente por operação. Configuração desconhecida deve permanecer negada por padrão.
- Use transações nas operações de escrita e consultas de leitura adequadas. Não devolver entidades JPA como JSON público.
- Não registre tokens, segredos ou dados pessoais completos. O código atual não define um framework separado de logging; use o logger suportado pelo projeto apenas quando necessário e com dados minimizados.

## REST

- Preserve os prefixos e contratos existentes (`/api/v1`, e `/api/v2` para produtos no contrato atual). Verifique `docs/api-contract.yaml` e controllers antes de alterar um endpoint.
- Mantenha semântica HTTP, códigos, paginação e Problem Details compatíveis com o contrato. Não altere contrato para contornar falha de teste.
- Parâmetros de ordenação e filtros devem ser validados antes de compor a consulta; nunca concatenar entrada em SQL.

## Angular e TypeScript

- Use Angular standalone e organização por feature (`catalog`, `auth`, `pages`, `shared`). Não introduza NgModules ou uma biblioteca de UI sem justificativa.
- Separe componentes de interface, serviços `HttpClient`, modelos e guards. Use tipos explícitos para payloads/respostas e não modele dinheiro como `number` quando contrato define string decimal.
- Guards controlam navegação e visibilidade; o servidor continua responsável por autorizar.
- O interceptor bearer deve preservar o escopo da URL de API já configurado e nunca anexar token a hosts externos/Keycloak.
- Formulários seguem os padrões Angular existentes, preservam valores em erro e aplicam regras de validação na interface sem substituir validação do backend.
- Mantenha testes próximos às features. Use scripts existentes do `package.json`; não invoque Vitest diretamente quando o executor Angular é o ambiente oficial do projeto.

## Princípios gerais

Escreva código legível, coeso e testável. Evite duplicação de regras, mas não crie frameworks internos antecipadamente. Aplique SOLID/DRY de forma proporcional ao domínio, preserve compatibilidade e prefira mudanças pequenas que possam ser revisadas.
