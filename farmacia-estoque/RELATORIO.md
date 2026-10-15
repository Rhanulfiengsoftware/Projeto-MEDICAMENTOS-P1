# Relatório — Sistema de Estoque de Medicamentos

**Disciplina:** Programação Orientada a Objetos III — **Avaliação:** P1
**Aluno(a):** ______________________  **Curso:** ______________________

## 1. Visão geral

Aplicação desktop em Java 17 com interface JavaFX (FXML/Scene Builder) para controlar o estoque de
medicamentos de uma farmácia, com persistência em CSV e relatórios gerenciais via Stream API.

## 2. Funcionalidades implementadas

**Interface gráfica (3 abas)**

- **Cadastro:** formulário com os dados do medicamento e do fornecedor; botões *Cadastrar* e *Limpar*.
  Erros de validação são listados juntos em um alerta; após o sucesso o formulário é limpo.
- **Medicamentos:** tabela com todos os registros (ordenável), consulta por código, botão
  *Listar todos* e exclusão do item selecionado com confirmação. Linhas vermelhas indicam produtos
  vencidos e linhas amarelas, estoque baixo.
- **Relatórios:** seleção do relatório em uma lista, resumo textual (contagens e totais) e tabela com o resultado.

**Operações:** cadastrar, excluir, consultar por código e listar.

**Persistência:** CSV com `;`, carregado na inicialização e regravado após cada operação.

**Relatórios (todos com Stream API) — os quatro previstos no enunciado:**

| Relatório | Operações de Stream |
|-----------|--------------------|
| Próximos ao vencimento (30 dias) | `filter` + `sorted` + `toList` |
| Estoque baixo (< 5) | `filter` + `sorted` + `toList` |
| Valor total por fornecedor | `groupingBy` + `mapToInt` + `map` + `reduce(BigDecimal.ZERO, BigDecimal::add)` + `sorted` |
| Controlados vs. não controlados | `sorted` + `partitioningBy` |
| (apoio) Valor total geral | `map` + `reduce` |

## 3. Decisões de design

- **Arquitetura em camadas** (model → repository → service → controller/view): a interface não conhece o
  CSV e as regras de negócio não conhecem o JavaFX, o que permite testá-las sem abrir a tela.
- **Interface `MedicamentoRepository`:** o serviço depende da abstração; trocar o CSV por um banco de
  dados exigiria apenas uma nova implementação.
- **Composição:** `Medicamento` possui um `Fornecedor` (conforme o enunciado). `equals/hashCode` usam o
  código (medicamento) e o CNPJ (fornecedor).
- **`BigDecimal` para dinheiro:** evita erros de arredondamento de `double`; o preço é normalizado para 2 casas.
- **Validação centralizada** na classe `Validador`, que **acumula todos os erros** em vez de parar no primeiro.
  O CNPJ tem o cálculo dos dois dígitos verificadores, e dados são normalizados (maiúsculas, apenas dígitos) antes de validar/gravar.
- **Tratamento de exceções:** hierarquia própria de exceções *checadas* (`FarmaciaException` →
  `ValidacaoException`, `PersistenciaException`). A interface captura cada tipo e mostra mensagens amigáveis.
- **Persistência segura:** a gravação é feita em arquivo temporário e depois substitui o original; se a
  escrita falhar, o estado em memória é revertido. Linha malformada no CSV gera erro claro (com o número
  da linha) em vez de ser ignorada, evitando perda silenciosa de dados.
- **Cache em memória (`LinkedHashMap` por código):** consulta por código em O(1) e unicidade garantida.
- **Injeção de dependências manual:** `Main` monta repositório → serviço → controlador usando
  `FXMLLoader.setControllerFactory`.
- **Colunas tipadas nas tabelas:** a ordenação respeita o tipo real (data, número) enquanto a célula exibe
  o valor formatado (dd/MM/aaaa, R$, CNPJ com máscara).
- **Testes automatizados (JUnit 5)** para validação, relatórios (Streams) e persistência CSV.

## 4. Limitações e melhorias futuras

- Não há edição de um medicamento existente (fora do escopo do enunciado); para alterar, exclui-se e recadastra-se.
- O fornecedor é digitado em cada cadastro; poderia existir um cadastro próprio de fornecedores com preenchimento automático pelo CNPJ.
- Exportação dos relatórios (PDF/CSV) e filtros combinados.
