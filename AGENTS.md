# Instruções para agentes

Este repositório implementa o ERP Core como monólito modular. Antes de qualquer alteração, leia este arquivo, a especificação da tarefa e o guia especializado relacionado.

## Fontes e estado

- Confirme decisões no código e na configuração atuais. Documentos de visão, requisitos e backlog podem descrever baselines anteriores; [docs/architecture.md](docs/architecture.md) registra o estado observado e separa o que existe do que está planejado.
- Não apresente implementação como validada sem executar a compilação e os testes exigidos. Registre comandos, ambiente, banco e resultados reais.
- Não invente requisitos, dados de teste, documentação normativa, versões ou resultados.

## Limites de implementação

- Preserve os limites de `com.erp.core.<modulo>` e evite dependências circulares ou acesso a internals de outros módulos.
- Mantenha regras de negócio no domínio ou na aplicação conforme o padrão do módulo. Controllers coordenam HTTP; não devolva entidades JPA como contratos externos.
- Inspecione os padrões existentes antes de introduzir abstrações, bibliotecas, configurações ou convenções novas.
- Implemente somente o escopo autorizado; preserve alterações preexistentes e não altere arquivos sem relação com a tarefa.
- Migrations Flyway compartilhadas são append-only: não edite migrations já aplicadas; crie uma nova migration para mudanças posteriores.
- Não remova nem desative testes para obter aprovação. Não faça commits sem autorização explícita.

## Segurança e dados

- O backend é a autoridade para autenticação/autorização; guards e visibilidade Angular são controles de interface.
- Não registre tokens, credenciais, documentos pessoais completos ou outros dados sensíveis. Não exponha detalhes internos em respostas de erro.
- Testes backend usam PostgreSQL real e devem apontar explicitamente para `erp_core_test`; nunca use o banco persistente `erp_core` como banco de teste.
- Execute comandos Windows somente no PowerShell nativo. Não invoque binários Windows pelo WSL.

## Fluxo de trabalho

ANÁLISE → PLANEJAMENTO → IMPLEMENTAÇÃO → TESTES → VALIDAÇÃO → RELATÓRIO → APROVAÇÃO.

Divida tarefas grandes. Não declare etapa validada enquanto qualquer verificação obrigatória estiver pendente, falha ou ignorada.

## Guias

- [Arquitetura atual](docs/architecture.md)
- [Padrões de código](docs/coding-standards.md)
- [Estratégia de testes](docs/testing-strategy.md)
- [Convenções de banco](docs/database-conventions.md)
- [Segurança](docs/security/README.md) e [modelo de segurança existente](docs/security-model.md)
- [Especificações](docs/specs/)
- [Prompts e aprovação](prompts/README.md)
