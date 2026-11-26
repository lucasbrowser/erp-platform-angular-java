# DOC-01 — Organização documental e configuração de agentes de IA do ERP Core

Atue como arquiteto de software sênior, especialista em Java 21, Spring Boot, Angular, PostgreSQL, Keycloak e arquitetura de monólito modular.

Seu objetivo é organizar a documentação do projeto ERP Core e estabelecer instruções permanentes para agentes de desenvolvimento, especialmente Codex.

IMPORTANTE: esta tarefa é exclusivamente documental. Não implementar funcionalidades, não alterar código de produção e não modificar o comportamento da aplicação.

## 1. Diagnóstico inicial obrigatório

Antes de criar ou modificar arquivos:

1. Inspecione a estrutura completa do repositório.
2. Leia o README.md existente.
3. Leia os documentos existentes em docs/.
4. Leia os registros de decisões arquiteturais em docs/decisions/.
5. Leia as especificações em docs/specs/.
6. Inspecione a estrutura de apps/api.
7. Inspecione a estrutura do frontend em apps/web.
8. Analise os arquivos de configuração Maven, Angular, Docker Compose e Keycloak.
9. Identifique os padrões efetivamente utilizados no código.
10. Identifique documentação duplicada, desatualizada ou contraditória.

Não presuma que uma tecnologia está implementada apenas porque aparece em um documento antigo.

Diferencie:
- Implementado e validado.
- Implementado, mas não validado.
- Planejado.
- Fora do escopo atual.

Não invente informações para preencher lacunas.

## 2. Estrutura documental desejada

Organize a documentação para seguir esta estrutura:

```text
erp-platform-angular-java/
├── AGENTS.md
├── docs/
│   ├── architecture.md
│   ├── coding-standards.md
│   ├── testing-strategy.md
│   ├── database-conventions.md
│   ├── decisions/
│   ├── specs/
│   └── security/
├── prompts/
│   ├── README.md
│   ├── T-01.md
│   ├── T-02.md
│   ├── T-03.md
│   ├── T-04.md
│   ├── T-05.md
│   └── T-06.md
├── apps/
├── infra/
└── README.md
```

Preserve os arquivos e diretórios existentes.

Não mova documentos automaticamente sem verificar referências e links.

Se algum diretório já existir, aproveite-o.

## 3. AGENTS.md — Instruções centrais

Crie o arquivo AGENTS.md na raiz do projeto.

Ele deverá ser conciso, objetivo e funcionar como manual operacional para agentes de IA.

Incluir:

### 3.1 Identidade do projeto

ERP Core:
- ERP desenvolvido como monólito modular.
- Backend Java 21 e Spring Boot.
- Frontend Angular.
- PostgreSQL.
- Flyway para migrations.
- Keycloak para identidade e autenticação.
- Spring Security para autorização.

Confirme as tecnologias e versões no repositório antes de registrar informações específicas.

### 3.2 Arquitetura

Estabeleça as regras arquiteturais efetivamente adotadas.

Orientações:
- Respeitar os limites dos módulos.
- Evitar dependências circulares.
- Manter regras de negócio no domínio ou na aplicação conforme a arquitetura definida.
- Não colocar regras de negócio em controllers.
- Separar contratos externos de entidades de persistência.
- Não criar abstrações desnecessárias.

### 3.3 Padrões de desenvolvimento

Estabeleça as regras para:
- Java.
- Spring Boot.
- Angular.
- TypeScript.
- REST APIs.
- Tratamento de exceções.
- Logging.
- Validação.
- Segurança.
- Persistência.

### 3.4 Regras obrigatórias para agentes

Todo agente deverá:

1. Ler AGENTS.md antes de modificar o projeto.
2. Consultar os documentos específicos da tarefa.
3. Inspecionar o código existente antes de implementar.
4. Respeitar a arquitetura e os contratos existentes.
5. Implementar somente o escopo solicitado.
6. Não alterar arquivos sem relação com a tarefa.
7. Não modificar migrations já aplicadas.
8. Não adicionar dependências sem justificativa.
9. Não remover testes para fazer uma validação passar.
10. Não ignorar falhas de compilação ou testes.
11. Não realizar commits sem autorização explícita.
12. Apresentar relatório técnico ao concluir cada etapa.

### 3.5 Ambiente de desenvolvimento

Registrar os padrões efetivamente confirmados:

- Windows 10.
- JDK 21.
- Maven 3.9.x.
- Node.js conforme configuração do projeto.
- PostgreSQL executado via Docker Compose.
- PowerShell nativo do Windows para comandos de validação.

Os testes backend devem utilizar o banco erp_core_test.

Não permitir que testes utilizem acidentalmente o banco erp_core.

Não executar binários Windows através do WSL.

### 3.6 Fluxo de trabalho

Estabelecer o seguinte ciclo:

ANÁLISE → PLANEJAMENTO → IMPLEMENTAÇÃO → TESTES → VALIDAÇÃO → RELATÓRIO → APROVAÇÃO.

Tarefas grandes devem ser divididas em partes menores.

Não iniciar a próxima parte enquanto a anterior estiver pendente de revisão ou validação.

## 4. architecture.md

Criar docs/architecture.md documentando:

- Visão geral do ERP.
- Arquitetura de monólito modular.
- Backend.
- Frontend.
- Persistência.
- Autenticação e autorização.
- Comunicação frontend/backend.
- Infraestrutura.
- Organização dos módulos.
- Fluxo de requisições.
- Decisões arquiteturais existentes.

Utilizar diagramas Mermaid quando ajudarem a explicar a arquitetura.

Não inventar módulos ou integrações que não estejam presentes.

Separar arquitetura atual de evolução planejada.

## 5. coding-standards.md

Criar docs/coding-standards.md com convenções para:

### Java
- Java 21.
- Organização de pacotes.
- Nomenclatura.
- Imutabilidade quando apropriada.
- Tratamento de exceções.
- Validação.
- Injeção de dependência.
- Separação de responsabilidades.
- DTOs.
- Testabilidade.

### Spring Boot
- Controllers.
- Services.
- Repositories.
- Configurações.
- Segurança.
- Tratamento global de erros.
- APIs REST.

### Angular
- Componentes.
- Services.
- Models.
- Routing.
- Guards.
- Interceptors.
- Formulários.
- Tipagem.
- Testes.

### Convenções gerais
- DRY.
- SOLID.
- Clean Code.
- Evitar overengineering.
- Código legível e testável.

Utilizar os padrões que já aparecem no projeto.

## 6. testing-strategy.md

Criar docs/testing-strategy.md.

Documentar:

- Pirâmide de testes.
- Testes unitários.
- Testes de integração.
- Testes de repositório.
- Testes de controllers.
- Testes de segurança.
- Testes frontend.
- Fixtures.
- Isolamento de dados.
- Critérios de aprovação.

### Backend

Documentar:
- JUnit.
- Mockito, se utilizado.
- AssertJ, se utilizado.
- Spring Boot Test.
- PostgreSQL real nos testes de integração.
- Configuração do banco erp_core_test.

Registrar os comandos reais de execução.

### Frontend

Documentar as ferramentas efetivamente utilizadas e os comandos disponíveis no package.json.

### Critérios

Uma tarefa não pode ser considerada validada quando:
- A compilação não foi executada.
- Os testes obrigatórios não foram executados.
- Existem testes ignorados indevidamente.
- Há falhas ou erros.
- O banco de testes não foi identificado.

## 7. database-conventions.md

Criar docs/database-conventions.md.

Documentar:

- PostgreSQL.
- Organização de schemas.
- Convenção de tabelas e colunas.
- Chaves primárias.
- Chaves estrangeiras.
- Índices.
- Constraints.
- Tipos de dados.
- Valores monetários.
- Datas e horários.
- Auditoria.
- Exclusão lógica, quando aplicável.
- Migrations Flyway.
- Estratégia de compatibilidade com Hibernate.

Incluir regras obrigatórias:

- Nunca editar migration já aplicada em ambiente compartilhado.
- Criar nova migration para mudanças posteriores.
- Não utilizar tipos aproximados para valores monetários.
- Preservar integridade referencial.
- Evitar consultas sem paginação em coleções potencialmente grandes.
- Separar banco de desenvolvimento e banco de testes.

## 8. prompts/README.md

Criar um índice dos prompts do projeto.

Documentar:
- Objetivo dos prompts.
- Padrão de nomenclatura T-XX.
- Relação com especificações.
- Divisão de tarefas grandes.
- Critérios de conclusão.
- Processo de aprovação.

## 9. prompts/T-01.md até T-06.md

Inspecione os documentos e registros existentes para identificar o conteúdo real das tarefas T-01 até T-06.

Regras:
- Não inventar o conteúdo de tarefas antigas.
- Não reconstruir decisões sem evidências.
- Se encontrar os prompts originais, organizá-los preservando seu conteúdo.
- Se não encontrar uma tarefa, criar apenas um arquivo de referência com status "Conteúdo original não localizado", sem inventar sua especificação.
- Não sobrescrever prompts existentes.
- Registrar relações com documentos e commits somente quando houver evidência no repositório.

## 10. Hierarquia e manutenção

O AGENTS.md da raiz deverá apontar para os documentos especializados.

Não duplicar todo o conteúdo dos documentos especializados no AGENTS.md.

Verifique se já existem outros arquivos AGENTS.md em subdiretórios.

Não criar instruções contraditórias entre arquivos.

Caso existam instruções específicas em subdiretórios, preservar e avaliar sua compatibilidade com as instruções da raiz.

## 11. Restrições

Esta tarefa é exclusivamente documental.

Não alterar:
- Código Java.
- Código Angular.
- Migrations.
- Dependências Maven.
- package.json.
- package-lock.json.
- Docker Compose.
- Configurações de autenticação.
- Arquivos de ambiente.

Não realizar refatorações.

Não executar commits.

## 12. Validação

Execute:

git diff --check

Verifique:
- Links relativos.
- Referências entre documentos.
- Nomes de arquivos.
- Consistência dos caminhos.
- Ausência de instruções contraditórias.

Não alterar arquivos preexistentes fora do escopo.

## 13. Relatório final

Apresente:

1. Estrutura documental encontrada inicialmente.
2. Arquivos criados.
3. Arquivos modificados.
4. Documentos preservados.
5. Decisões arquiteturais registradas.
6. Informações que não puderam ser confirmadas.
7. Resultado de git diff --check.
8. Confirmação de que nenhum código de produção foi alterado.
9. Sugestões para evolução da documentação.

Não realizar commit automaticamente.