# Prompts de implementação

## Objetivo

Este diretório indexa instruções de trabalho e registros de tarefas usados no ERP Core. Um prompt orienta uma etapa; não substitui especificação funcional, código, revisão ou evidência de validação.

## Organização

- Arquivos `T-XX.md` são referências arquivísticas de tarefas antigas. Seu conteúdo só deve ser preenchido quando o prompt original ou evidência equivalente estiver disponível no repositório.
- Prompts de etapa podem manter nomes descritivos, como `implementacao-ordenacao-clientes.md`. Não renomeie nem sobrescreva esses arquivos sem verificar referências e preservar alterações existentes.
- Especificações funcionais e técnicas ficam em `docs/specs/`; a tarefa deve apontar para a especificação pertinente e indicar dependências/pré-condições.
- Divida trabalho amplo em etapas pequenas, com escopo, restrições, validação e relatório por etapa. Não iniciar etapa posterior se a anterior estiver sem revisão ou validação.

## Conclusão e aprovação

Uma etapa só termina quando todos os critérios do prompt foram tratados, validações obrigatórias foram realmente executadas, limitações foram declaradas e os arquivos alterados foram informados. Não relatar validação por inferência. Não criar commit automaticamente. A aprovação/revisão humana é posterior ao relatório; prompt não autoriza ignorar falhas ou alterar escopo.

## Índice de tarefas antigas

Os prompts originais T-01 a T-06 não foram localizados nos arquivos disponíveis nem na história Git inspecionada. As referências abaixo registram essa lacuna sem reconstruir conteúdo.

| Arquivo | Estado | Conteúdo localizado |
|---|---|---|
| [T-01](T-01.md) | Original não localizado | Não reconstruído |
| [T-02](T-02.md) | Original não localizado | Não reconstruído |
| [T-03](T-03.md) | Original não localizado | Não reconstruído |
| [T-04](T-04.md) | Original não localizado | Não reconstruído |
| [T-05](T-05.md) | Original não localizado | Não reconstruído |
| [T-06](T-06.md) | Original não localizado | Não reconstruído |

## Prompts preservados

- `implementacao-ordenacao-clientes.md` — prompt de ordenação de clientes existente; seu conteúdo e histórico local foram preservados.
- `implementacao-doc-agents.md` — especificação DOC-01 usada para organizar a documentação.
