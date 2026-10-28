# Regras de negócio (baseline consolidada)

As regras 1–8 são a baseline transversal do MVP. As regras 9–12 detalham o catálogo e foram implementadas na Etapa 3.1.

1. **Uma empresa:** o MVP atende uma única empresa. Cadastros não carregam `tenant_id`; multi-tenancy está fora do escopo.
2. **Autorização no servidor:** cada operação futura exige permissão conferida pela API. A interface não é uma barreira de segurança.
3. **Estoque negativo proibido:** uma venda só pode ser confirmada se houver saldo suficiente para todos os itens; confirmação deve falhar integralmente caso contrário. Reservas, concorrência, devoluções, ajustes e arredondamentos ainda precisam de detalhamento antes da implementação.
4. **Baixas manuais:** contas a pagar e a receber serão liquidadas por baixa manual. Integração bancária automática não faz parte do MVP.
5. **Fiscal fora do escopo:** emissão fiscal não faz parte do MVP.
6. **Consistência:** operação não está concluída se a transação persistente falhar; regras de consistência devem ser aplicadas no backend.
7. **Auditoria:** mudanças relevantes devem registrar principal autenticado, momento e ação, com retenção a especificar.
8. **Migrações:** migrações compartilhadas são append-only e não removem/regravam dados sem plano de compatibilidade aprovado.
9. **Categoria:** nome obrigatório após trim, até 120 caracteres e único sem diferenciar maiúsculas/minúsculas. Desativar preserva categoria e produtos associados; categorias inativas continuam consultáveis/listáveis.
10. **Produto:** SKU obrigatório até 64 caracteres, normalizado por trim e maiúsculas com locale independente, e único após normalização. Nome obrigatório até 160 caracteres; preço é NUMERIC(14,2), não negativo e representado por BigDecimal.
11. **Categoria de produto:** criar ou mudar a associação exige categoria existente e ativa. Produtos já associados a categoria posteriormente desativada permanecem visíveis e podem ser atualizados mantendo essa mesma associação; nova associação à categoria inativa retorna conflito.
12. **Estados e consultas:** criar inicia categoria/produto como ativo; desativar é lógico e retorna 204. Consultas por ID e listas incluem inativos; produto pode ser filtrado por active. Não há exclusão física nem operação de reativação nesta etapa.

Perfis iniciais e acesso por operação estão em [Modelo de segurança](security-model.md). Estados, exceções, segregação de funções e limites de aprovação dos módulos futuros permanecem para detalhamento com stakeholders.
