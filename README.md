Farmácia — Controle de Estoque de Medicamentos
Sistema desktop em Java 17 + JavaFX 17 (interface em FXML, editável no Scene Builder) para gerenciar o estoque de medicamentos de uma farmácia. Trabalho P1 — Programação Orientada a Objetos III (UNIFAN).

Funcionalidades
Cadastrar medicamento (com fornecedor) com validação completa dos dados
Excluir medicamento (com confirmação)
Consultar medicamento por código
Listar todos os medicamentos (tabela ordenável por qualquer coluna)
Persistência em arquivo CSV separado por ponto-e-vírgula (data/medicamentos.csv)
Relatórios com Stream API (filter, map, reduce, sorted, groupingBy, partitioningBy):
Medicamentos próximos ao vencimento (próximos 30 dias)
Medicamentos com estoque baixo (menos de 5 unidades)
Valor total do estoque por fornecedor
Controlados vs. não controlados
Requisitos
Ferramenta	Versão
JDK	17 ou superior
Maven	3.8 ou superior
O JavaFX é baixado automaticamente pelo Maven (não precisa instalar o SDK separadamente).

Como executar
# 1. entre na pasta do projeto
cd farmacia-estoque

# 2. execute a aplicação
mvn javafx:run
Para rodar os testes automatizados:

mvn test
Executando pelo IDE
IntelliJ IDEA: File > Open na pasta do projeto (ele reconhece o pom.xml). Abra o painel Maven e execute farmacia-estoque > Plugins > javafx > javafx:run. Alternativa: execute a classe br.edu.unifan.farmacia.Launcher.

Eclipse / VS Code: importe como projeto Maven existente e execute mvn javafx:run (ou a classe Launcher).

O arquivo de dados é criado/lido em data/medicamentos.csv, relativo à pasta de onde o programa é executado (a raiz do projeto, ao usar mvn javafx:run). Se o arquivo não existir, ele é criado automaticamente. O arquivo incluído no projeto contém 6 medicamentos de exemplo (pode ser apagado para começar do zero).

Editando a tela no Scene Builder
Abra o Scene Builder.
File > Open... e selecione src/main/resources/view/main.fxml.
O controlador (br.edu.unifan.farmacia.controller.MainController) já está associado e todos os fx:id e eventos (onAction) estão definidos no FXML.
As colunas das tabelas são criadas no controlador (para formatar datas/moeda e ordenar pelo tipo real do dado); no Scene Builder aparecem apenas os TableView.
No IntelliJ: clique com o botão direito em main.fxml > Open in SceneBuilder.

Formato do arquivo CSV
Codificação UTF-8, separador ;, primeira linha com o cabeçalho:

codigo;nome;descricao;principioAtivo;dataValidade;quantidadeEstoque;preco;controlado;cnpj;razaoSocial;telefone;email;cidade;estado
AMX0001;Amoxicilina 500mg;Antibiótico...;Amoxicilina;2027-03-15;40;32.90;false;11222333000181;Distribuidora...;6232221100;vendas@...;Goiânia;GO
Data no formato ISO (aaaa-mm-dd), preço com ponto decimal, CNPJ/telefone só com dígitos.
Se o texto de um campo contiver ;, ele é trocado por , (e quebras de linha por espaço) ao gravar.
Regras de validação
Campo	Regra
Código	exatamente 7 caracteres alfanuméricos (salvo em maiúsculas); único
Nome	não vazio, mínimo de 3 caracteres
Princípio ativo	obrigatório
Data de validade	obrigatória; não pode ser data passada (hoje é aceito)
Quantidade	inteiro ≥ 0
Preço	valor positivo (aceita 12,90, 12.90, R$ 1.234,50)
CNPJ	14 dígitos (com ou sem máscara), não pode ser sequência repetida, dígitos verificadores conferidos
Razão social / Cidade	obrigatórias
Telefone	DDD + número (10 ou 11 dígitos)
E-mail	formato válido
Estado	UF brasileira válida
Todos os erros são exibidos de uma só vez em um alerta.

Estrutura do projeto
farmacia-estoque/
├── pom.xml
├── README.md
├── RELATORIO.md
├── data/medicamentos.csv                    # base de dados (CSV)
└── src
    ├── main
    │   ├── java/br/edu/unifan/farmacia
    │   │   ├── Main.java / Launcher.java    # inicialização JavaFX
    │   │   ├── model/                       # Fornecedor, Medicamento
    │   │   ├── validation/                  # Validador (regras + CNPJ)
    │   │   ├── repository/                  # interface + implementação CSV
    │   │   ├── service/                     # MedicamentoService, RelatorioService (Streams)
    │   │   ├── controller/                  # MainController (eventos da tela)
    │   │   └── exception/                   # FarmaciaException, ValidacaoException, PersistenciaException
    │   └── resources
    │       ├── view/main.fxml               # tela (Scene Builder)
    │       └── css/style.css
    └── test/java/br/edu/unifan/farmacia     # testes JUnit 5
Publicando no GitHub
git init
git add .
git commit -m "P1 - Sistema de estoque de medicamentos (JavaFX)"
git branch -M main
git remote add origin https://github.com/<seu-usuario>/<seu-repositorio>.git
git push -u origin main
