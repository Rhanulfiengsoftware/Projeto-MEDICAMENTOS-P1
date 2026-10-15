package br.edu.unifan.farmacia.controller;

import br.edu.unifan.farmacia.exception.FarmaciaException;
import br.edu.unifan.farmacia.exception.ValidacaoException;
import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;
import br.edu.unifan.farmacia.service.MedicamentoService;
import br.edu.unifan.farmacia.service.RelatorioService;
import br.edu.unifan.farmacia.service.RelatorioService.ValorEstoqueFornecedor;
import br.edu.unifan.farmacia.validation.Validador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Controlador da tela principal (main.fxml): trata os eventos do formulário
 * de cadastro, da tabela de listagem/consulta/exclusão e da exibição dos
 * relatórios.
 */
public class MainController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private static final List<String> RELATORIOS = List.of(
            "Medicamentos próximos ao vencimento (próximos 30 dias)",
            "Medicamentos com estoque baixo (menos de 5 unidades)",
            "Valor total do estoque por fornecedor",
            "Medicamentos controlados vs. não controlados");

    private final MedicamentoService service;
    private final RelatorioService relatorioService;

    private final ObservableList<Medicamento> listaMedicamentos = FXCollections.observableArrayList();
    private final ObservableList<Medicamento> listaRelatorio = FXCollections.observableArrayList();
    private final ObservableList<ValorEstoqueFornecedor> listaFornecedores = FXCollections.observableArrayList();

    // ---- Formulário: medicamento ----
    @FXML private TextField tfCodigo;
    @FXML private TextField tfNome;
    @FXML private TextField tfPrincipioAtivo;
    @FXML private DatePicker dpValidade;
    @FXML private TextField tfQuantidade;
    @FXML private TextField tfPreco;
    @FXML private CheckBox chkControlado;
    @FXML private TextArea taDescricao;

    // ---- Formulário: fornecedor ----
    @FXML private TextField tfCnpj;
    @FXML private TextField tfRazaoSocial;
    @FXML private TextField tfTelefone;
    @FXML private TextField tfEmail;
    @FXML private TextField tfCidade;
    @FXML private ComboBox<String> cbEstado;

    // ---- Listagem ----
    @FXML private TextField tfBuscaCodigo;
    @FXML private TableView<Medicamento> tbMedicamentos;
    @FXML private Button btnExcluir;
    @FXML private Label lblStatus;

    // ---- Relatórios ----
    @FXML private ComboBox<String> cbRelatorio;
    @FXML private Label lblResumoRelatorio;
    @FXML private TableView<Medicamento> tbRelatorioMedicamentos;
    @FXML private TableView<ValorEstoqueFornecedor> tbRelatorioFornecedores;

    public MainController(MedicamentoService service, RelatorioService relatorioService) {
        this.service = service;
        this.relatorioService = relatorioService;
    }

    // =====================================================================
    // Inicialização
    // =====================================================================

    @FXML
    private void initialize() {
        cbEstado.setItems(FXCollections.observableArrayList(
                Validador.UFS.stream().sorted().toList()));

        dpValidade.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {
                return data == null ? "" : FORMATO_DATA.format(data);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.trim(), FORMATO_DATA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });

        configurarTabelaMedicamentos(tbMedicamentos, listaMedicamentos);
        configurarTabelaMedicamentos(tbRelatorioMedicamentos, listaRelatorio);
        configurarTabelaFornecedores();

        tbMedicamentos.setPlaceholder(new Label("Nenhum medicamento para exibir."));
        btnExcluir.disableProperty().bind(
                tbMedicamentos.getSelectionModel().selectedItemProperty().isNull());

        cbRelatorio.setItems(FXCollections.observableArrayList(RELATORIOS));
        cbRelatorio.getSelectionModel().selectFirst();
        exibirTabelaFornecedores(false);
        lblResumoRelatorio.setText("Escolha um relatório e clique em \"Gerar relatório\".");

        atualizarListagem();
    }

    // =====================================================================
    // Eventos: cadastro
    // =====================================================================

    @FXML
    private void onSalvar() {
        try {
            Medicamento medicamento = lerFormulario();
            service.cadastrar(medicamento);

            limparFormulario();
            atualizarListagem();
            definirStatus("Medicamento " + medicamento.getCodigo() + " cadastrado com sucesso.");
            mostrarAlerta(Alert.AlertType.INFORMATION, "Cadastro realizado",
                    "Medicamento " + medicamento.getCodigo() + " - " + medicamento.getNome()
                            + " cadastrado com sucesso.");
        } catch (ValidacaoException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Dados inválidos",
                    "Corrija os itens abaixo:\n\n• " + String.join("\n• ", e.getErros()));
        } catch (FarmaciaException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro ao salvar", e.getMessage());
        }
    }

    @FXML
    private void onLimpar() {
        limparFormulario();
    }

    // =====================================================================
    // Eventos: listagem / consulta / exclusão
    // =====================================================================

    @FXML
    private void onConsultar() {
        String codigo = tfBuscaCodigo.getText();
        if (codigo == null || codigo.isBlank()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Consulta", "Informe o código do medicamento.");
            return;
        }
        Optional<Medicamento> encontrado = service.consultar(codigo);
        if (encontrado.isPresent()) {
            listaMedicamentos.setAll(encontrado.get());
            tbMedicamentos.getSelectionModel().selectFirst();
            definirStatus("Medicamento encontrado.");
        } else {
            listaMedicamentos.clear();
            definirStatus("Nenhum medicamento com o código " + codigo.trim().toUpperCase() + ".");
            mostrarAlerta(Alert.AlertType.INFORMATION, "Consulta",
                    "Medicamento com código " + codigo.trim().toUpperCase() + " não encontrado.");
        }
    }

    @FXML
    private void onListarTodos() {
        tfBuscaCodigo.clear();
        atualizarListagem();
    }

    @FXML
    private void onExcluir() {
        Medicamento selecionado = tbMedicamentos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exclusão", "Selecione um medicamento na tabela.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir o medicamento " + selecionado.getCodigo()
                        + " - " + selecionado.getNome() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmacao.setTitle("Confirmar exclusão");
        confirmacao.setHeaderText(null);
        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            service.excluir(selecionado.getCodigo());
            tfBuscaCodigo.clear();
            atualizarListagem();
            definirStatus("Medicamento " + selecionado.getCodigo() + " excluído.");
        } catch (FarmaciaException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro ao excluir", e.getMessage());
        }
    }

    // =====================================================================
    // Eventos: relatórios
    // =====================================================================

    @FXML
    private void onGerarRelatorio() {
        int opcao = cbRelatorio.getSelectionModel().getSelectedIndex();
        if (opcao < 0) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Relatório", "Selecione um relatório.");
            return;
        }

        List<Medicamento> todos = service.listar();
        LocalDate hoje = LocalDate.now();

        switch (opcao) {
            case 0 -> {
                List<Medicamento> lista = relatorioService.proximosAoVencimento(todos, hoje);
                exibirTabelaFornecedores(false);
                listaRelatorio.setAll(lista);
                lblResumoRelatorio.setText(lista.size() + " medicamento(s) vencem até "
                        + FORMATO_DATA.format(hoje.plusDays(RelatorioService.DIAS_VENCIMENTO_PROXIMO))
                        + " (próximos " + RelatorioService.DIAS_VENCIMENTO_PROXIMO + " dias).");
            }
            case 1 -> {
                List<Medicamento> lista = relatorioService.estoqueBaixo(todos);
                exibirTabelaFornecedores(false);
                listaRelatorio.setAll(lista);
                lblResumoRelatorio.setText(lista.size() + " medicamento(s) com menos de "
                        + RelatorioService.LIMITE_ESTOQUE_BAIXO + " unidades em estoque.");
            }
            case 2 -> {
                List<ValorEstoqueFornecedor> linhas = relatorioService.valorTotalPorFornecedor(todos);
                exibirTabelaFornecedores(true);
                listaFornecedores.setAll(linhas);
                lblResumoRelatorio.setText(linhas.size() + " fornecedor(es) — valor total do estoque: "
                        + MOEDA.format(relatorioService.valorTotalGeral(todos)));
            }
            case 3 -> {
                Map<Boolean, List<Medicamento>> grupos = relatorioService.controladosVersusNaoControlados(todos);
                List<Medicamento> controlados = grupos.get(true);
                List<Medicamento> naoControlados = grupos.get(false);
                List<Medicamento> exibicao = new ArrayList<>(controlados);
                exibicao.addAll(naoControlados);
                exibirTabelaFornecedores(false);
                listaRelatorio.setAll(exibicao);
                lblResumoRelatorio.setText("Controlados: " + controlados.size()
                        + "  |  Não controlados: " + naoControlados.size()
                        + "  (controlados listados primeiro)");
            }
            default -> { }
        }
    }

    // =====================================================================
    // Formulário
    // =====================================================================

    private Medicamento lerFormulario() throws ValidacaoException {
        Fornecedor fornecedor = new Fornecedor(
                tfCnpj.getText(), tfRazaoSocial.getText(), tfTelefone.getText(),
                tfEmail.getText(), tfCidade.getText(), cbEstado.getValue());

        return new Medicamento(
                tfCodigo.getText(), tfNome.getText(), taDescricao.getText(), tfPrincipioAtivo.getText(),
                dpValidade.getValue(), lerQuantidade(), lerPreco(), chkControlado.isSelected(), fornecedor);
    }

    private int lerQuantidade() throws ValidacaoException {
        String texto = tfQuantidade.getText() == null ? "" : tfQuantidade.getText().trim();
        if (texto.isEmpty()) {
            throw new ValidacaoException("Quantidade em estoque é obrigatória.");
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Quantidade em estoque deve ser um número inteiro.");
        }
    }

    /** Aceita "12,90", "12.90" e "1.234,56" (com ou sem "R$"). */
    private BigDecimal lerPreco() throws ValidacaoException {
        String texto = tfPreco.getText() == null ? "" : tfPreco.getText().replace("R$", "").trim();
        if (texto.isEmpty()) {
            throw new ValidacaoException("Preço é obrigatório.");
        }
        if (texto.contains(",")) {
            texto = texto.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Preço inválido: informe um valor numérico (ex.: 12,90).");
        }
    }

    private void limparFormulario() {
        tfCodigo.clear();
        tfNome.clear();
        tfPrincipioAtivo.clear();
        dpValidade.setValue(null);
        dpValidade.getEditor().clear();
        tfQuantidade.clear();
        tfPreco.clear();
        chkControlado.setSelected(false);
        taDescricao.clear();
        tfCnpj.clear();
        tfRazaoSocial.clear();
        tfTelefone.clear();
        tfEmail.clear();
        tfCidade.clear();
        cbEstado.setValue(null);
        tfCodigo.requestFocus();
    }

    // =====================================================================
    // Tabelas
    // =====================================================================

    private void atualizarListagem() {
        List<Medicamento> todos = service.listar();
        listaMedicamentos.setAll(todos);
        definirStatus(todos.size() + " medicamento(s) cadastrado(s).");
    }

    private void configurarTabelaMedicamentos(TableView<Medicamento> tabela, ObservableList<Medicamento> dados) {
        List<TableColumn<Medicamento, ?>> colunas = new ArrayList<>();
        colunas.add(coluna("Código", 85, Medicamento::getCodigo, String::valueOf));
        colunas.add(coluna("Nome", 160, Medicamento::getNome, String::valueOf));
        colunas.add(coluna("Princípio ativo", 130, Medicamento::getPrincipioAtivo, String::valueOf));
        colunas.add(coluna("Validade", 90, Medicamento::getDataValidade, FORMATO_DATA::format));
        colunas.add(coluna("Estoque", 70, Medicamento::getQuantidadeEstoque, String::valueOf));
        colunas.add(coluna("Preço", 90, Medicamento::getPreco, MOEDA::format));
        colunas.add(coluna("Controlado", 85, Medicamento::isControlado, c -> c ? "Sim" : "Não"));
        colunas.add(coluna("Fornecedor", 160, m -> m.getFornecedor().getRazaoSocial(), String::valueOf));
        colunas.add(coluna("CNPJ", 140, m -> m.getFornecedor().getCnpj(), Validador::formatarCnpj));
        colunas.add(coluna("Descrição", 200, Medicamento::getDescricao, String::valueOf));

        tabela.getColumns().setAll(colunas);
        tabela.setItems(dados);
        tabela.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabela.setRowFactory(tv -> criarLinhaComDestaque());
    }

    private void configurarTabelaFornecedores() {
        List<TableColumn<ValorEstoqueFornecedor, ?>> colunas = new ArrayList<>();
        colunas.add(coluna("CNPJ", 160, ValorEstoqueFornecedor::cnpj, Validador::formatarCnpj));
        colunas.add(coluna("Razão social", 320, ValorEstoqueFornecedor::razaoSocial, String::valueOf));
        colunas.add(coluna("Unidades em estoque", 150, ValorEstoqueFornecedor::totalUnidades, String::valueOf));
        colunas.add(coluna("Valor total do estoque", 180, ValorEstoqueFornecedor::valorTotal, MOEDA::format));

        tbRelatorioFornecedores.getColumns().setAll(colunas);
        tbRelatorioFornecedores.setItems(listaFornecedores);
        tbRelatorioFornecedores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbRelatorioFornecedores.setPlaceholder(new Label("Nenhum dado para exibir."));
    }

    /**
     * Cria uma coluna tipada (para que a ordenação use o tipo real: número,
     * data, etc.) e formata o valor exibido na célula.
     */
    private <S, T> TableColumn<S, T> coluna(String titulo, double largura,
                                            Function<S, T> extrator, Function<T, String> formatador) {
        TableColumn<S, T> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(extrator.apply(c.getValue())));
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : formatador.apply(item));
            }
        });
        return coluna;
    }

    /** Linha vermelha = vencido; amarela = estoque baixo. */
    private TableRow<Medicamento> criarLinhaComDestaque() {
        return new TableRow<>() {
            @Override
            protected void updateItem(Medicamento m, boolean vazio) {
                super.updateItem(m, vazio);
                getStyleClass().removeAll("row-vencido", "row-estoque-baixo");
                if (vazio || m == null) {
                    return;
                }
                if (m.getDataValidade().isBefore(LocalDate.now())) {
                    getStyleClass().add("row-vencido");
                } else if (m.getQuantidadeEstoque() < RelatorioService.LIMITE_ESTOQUE_BAIXO) {
                    getStyleClass().add("row-estoque-baixo");
                }
            }
        };
    }

    private void exibirTabelaFornecedores(boolean exibir) {
        tbRelatorioFornecedores.setVisible(exibir);
        tbRelatorioFornecedores.setManaged(exibir);
        tbRelatorioMedicamentos.setVisible(!exibir);
        tbRelatorioMedicamentos.setManaged(!exibir);
    }

    // =====================================================================
    // Utilitários de interface
    // =====================================================================

    private void definirStatus(String mensagem) {
        lblStatus.setText(mensagem);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
