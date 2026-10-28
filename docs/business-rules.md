# Regras de negócio (baseline consolidada)

Estas regras foram confirmadas para o MVP e guiam o desenho futuro. Não implementam processos nesta fase.

1. **Uma empresa:** o MVP atende uma única empresa. Cadastros não carregam `tenant_id`; multi-tenancy está fora do escopo.
2. **Autorização no servidor:** cada operação futura exige permissão conferida pela API. A interface não é uma barreira de segurança.
3. **Estoque negativo proibido:** uma venda só pode ser confirmada se houver saldo suficiente para todos os itens; confirmação deve falhar integralmente caso contrário. Reservas, concorrência, devoluções, ajustes e arredondamentos ainda precisam de detalhamento antes da implementação.
4. **Baixas manuais:** contas a pagar e a receber serão liquidadas por baixa manual. Integração bancária automática não faz parte do MVP.
5. **Fiscal fora do escopo:** emissão fiscal não faz parte do MVP.
6. **Consistência:** operação não está concluída se a transação persistente falhar; regras de consistência devem ser aplicadas no backend.
7. **Auditoria:** mudanças relevantes devem registrar principal autenticado, momento e ação, com retenção a especificar.
8. **Migrações:** migrações compartilhadas são append-only e não removem/regravam dados sem plano de compatibilidade aprovado.

Perfis iniciais e acesso por operação estão em [Modelo de segurança](security-model.md). Estados, exceções, segregação de funções e limites de aprovação permanecem para detalhamento com stakeholders antes de código de negócio.
