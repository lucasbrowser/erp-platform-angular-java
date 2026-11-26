# C-02.3-B1 — Implementação da ordenação segura de Clientes

Atue como engenheiro de software sênior, especialista em Java 21, Spring Boot, Spring Data JPA, PostgreSQL e arquitetura de monólito modular.

Implemente a primeira parte da C-02.3-B do módulo Clientes do ERP Core.

## 1. Contexto

O módulo Clientes já possui:

- Entidade Customer.
- Repositório CustomerRepository com JpaSpecificationExecutor<Customer>.
- CustomerSpecifications para filtros dinâmicos.
- Consultas paginadas com Page<Customer>.
- Testes de integração CustomerSearchIntegrationTest com 23 cenários aprovados.
- Regras de domínio PF/PJ.
- Normalização e validação de CPF/CNPJ.

A etapa C-02.3-A foi concluída, validada e commitada.

Antes de implementar:

1. Leia docs/specs/customers.md.
2. Inspecione CustomerRepository.
3. Inspecione CustomerSpecifications.
4. Inspecione CustomerSearchIntegrationTest.
5. Analise como o módulo Catalog implementa paginação e ordenação.
6. Identifique os padrões existentes para validação de parâmetros.

Não replique estruturas que já existam no projeto sem necessidade.

## 2. Objetivo

Implementar uma política segura de ordenação para consultas de clientes.

A ordenação deverá aceitar somente os campos explicitamente permitidos e suportar múltiplos critérios.

## 3. Allowlist obrigatória

Permitir exclusivamente os seguintes campos:

- name
- document
- personType
- active
- createdAt
- updatedAt

Os nomes recebidos devem ser comparados com a allowlist de forma exata.

Não aceitar:
- Propriedades internas da entidade não previstas.
- Caminhos aninhados.
- Expressões JPQL.
- Fragmentos SQL.
- Funções arbitrárias.
- Nomes de propriedades concatenados diretamente em consultas.

A ordenação deve ser construída utilizando objetos Sort e Pageable do Spring Data.

## 4. Direção da ordenação

Aceitar:

- asc
- desc

Definir o comportamento padrão conforme docs/specs/customers.md.

Se a especificação não determinar um padrão, utilize name,asc e documente a decisão.

A direção deve ser validada sem permitir valores arbitrários.

## 5. Múltiplos critérios

Permitir que a consulta receba mais de um critério de ordenação.

Exemplo conceitual:

sort=name,asc
sort=createdAt,desc

A implementação deverá preservar a ordem de precedência dos critérios recebidos.

Não limitar a ordenação a um único campo.

## 6. Validação

Criar uma estrutura responsável por converter os parâmetros recebidos em Sort seguro.

A responsabilidade deve ficar isolada, preferencialmente em uma classe da camada application, seguindo os padrões do projeto.

Requisitos:

- Rejeitar campos fora da allowlist.
- Rejeitar direções inválidas.
- Não ignorar silenciosamente critérios inválidos.
- Não converter valores inválidos para campos padrão.
- Retornar uma exceção de validação compatível com o tratamento de erros existente.
- Não incluir informações sensíveis em mensagens de erro.

A futura API deverá transformar parâmetros inválidos em HTTP 400.

Não implementar controllers nesta etapa.

## 7. Integração com paginação

A estrutura deve permitir combinar:

- Specification<Customer>.
- Pageable.
- Sort validado.

Preservar o comportamento já implementado em CustomerRepository.

Não alterar a lógica dos filtros existentes.

Não carregar todos os registros em memória para ordenar.

A ordenação deve ser executada pelo PostgreSQL.

## 8. Segurança

A allowlist é uma fronteira de segurança.

Não utilizar:
- SQL dinâmico concatenado.
- JPQL dinâmico com entrada não validada.
- Reflection para aceitar qualquer propriedade.
- Conversão automática de nomes recebidos em propriedades da entidade.

A validação deve acontecer antes da criação do Pageable.

A ordenação por document deve ser permitida conforme a allowlist, mas não deve expor o documento em respostas.

A proteção de FINANCE contra acesso ao documento continuará sendo responsabilidade da futura camada de aplicação e das projeções.

## 9. Restrições

Não implementar nesta etapa:

- Controllers REST.
- CRUD.
- DTOs de resposta.
- Autorização por perfil.
- Projeções específicas de FINANCE.
- Alterações nas migrations.
- Alterações na entidade Customer sem justificativa técnica.
- Novas dependências sem justificativa.

Não modificar as Specifications dos filtros, salvo necessidade técnica comprovada.

Não alterar os testes existentes de filtros sem justificativa.

## 10. Testes nesta parte

Crie somente testes unitários da política de ordenação, caso sejam necessários para validar:

- Campos permitidos.
- Campo não permitido.
- Direções ASC e DESC.
- Direção inválida.
- Múltiplos critérios.
- Ordem de precedência.
- Valores ausentes e comportamento padrão.

Os testes de integração com PostgreSQL serão realizados na C-02.3-B2.

## 11. Relatório obrigatório

Ao finalizar, informe:

1. Arquivos criados e modificados.
2. Campos incluídos na allowlist.
3. Estratégia de validação.
4. Como múltiplos critérios são processados.
5. Como a ordenação é integrada ao Pageable.
6. Tratamento de parâmetros inválidos.
7. Testes unitários criados e resultados.
8. Resultado de git diff --check.
9. Confirmação de que nenhuma migration foi alterada.
10. Pendências para a Parte B2.

Não realize commit automaticamente.

Não execute binários Windows pelo WSL. Utilize PowerShell nativo do Windows para validações que dependam do ambiente Windows.