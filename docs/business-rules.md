# Regras de negócio (baseline)

Não há regras transacionais de negócio implementadas ou aprovadas para esta fase.

1. Todo dado persistido no futuro deverá pertencer a uma organização/tenant se o produto validar o requisito de multiempresa; o modelo e o isolamento ainda precisam ser decididos.
2. Mudanças relevantes deverão ser atribuíveis a um principal autenticado.
3. Nenhuma operação de negócio poderá ser considerada concluída se sua transação persistente falhar.
4. Autorização deverá ser verificada no servidor em cada operação, independentemente do estado da interface.
5. Migrações de banco não devem remover ou reescrever dados existentes sem plano de compatibilidade aprovado.

Estas são restrições arquiteturais provisórias, não política fiscal, comercial ou de estoque. A fase de descoberta deve especificar regras, exceções, arredondamentos, estados e retenção de auditoria antes da implementação funcional.
