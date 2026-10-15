package br.edu.unifan.farmacia.repository;

import br.edu.unifan.farmacia.exception.PersistenciaException;
import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistência em arquivo CSV (UTF-8) com campos separados por ponto-e-vírgula.
 *
 * <ul>
 *   <li>Os dados são carregados do arquivo na criação do repositório;</li>
 *   <li>cada alteração (salvar/remover) regrava o arquivo imediatamente;</li>
 *   <li>a gravação é feita em arquivo temporário + substituição, evitando
 *       corromper o CSV caso ocorra uma falha no meio da escrita;</li>
 *   <li>se a gravação falhar, o estado em memória é revertido.</li>
 * </ul>
 */
public class MedicamentoCsvRepository implements MedicamentoRepository {

    private static final String SEPARADOR = ";";
    private static final int TOTAL_CAMPOS = 14;
    private static final String CABECALHO = String.join(SEPARADOR,
            "codigo", "nome", "descricao", "principioAtivo", "dataValidade",
            "quantidadeEstoque", "preco", "controlado",
            "cnpj", "razaoSocial", "telefone", "email", "cidade", "estado");

    private final Path arquivo;
    private final Map<String, Medicamento> cache = new LinkedHashMap<>();

    public MedicamentoCsvRepository(Path arquivo) throws PersistenciaException {
        this.arquivo = arquivo;
        carregar();
    }

    // ------------------------------------------------------------------
    // Operações do repositório
    // ------------------------------------------------------------------

    @Override
    public boolean existe(String codigo) {
        return cache.containsKey(codigo);
    }

    @Override
    public void salvar(Medicamento medicamento) throws PersistenciaException {
        Map<String, Medicamento> backup = new LinkedHashMap<>(cache);
        cache.put(medicamento.getCodigo(), medicamento);
        persistirOuReverter(backup);
    }

    @Override
    public boolean remover(String codigo) throws PersistenciaException {
        if (!cache.containsKey(codigo)) {
            return false;
        }
        Map<String, Medicamento> backup = new LinkedHashMap<>(cache);
        cache.remove(codigo);
        persistirOuReverter(backup);
        return true;
    }

    @Override
    public Optional<Medicamento> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(cache.get(codigo));
    }

    @Override
    public List<Medicamento> listarTodos() {
        return List.copyOf(cache.values());
    }

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    private void carregar() throws PersistenciaException {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            if (!Files.exists(arquivo)) {
                gravar(new ArrayList<>());          // cria o arquivo apenas com o cabeçalho
                return;
            }
            List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            for (int i = 1; i < linhas.size(); i++) {   // linha 0 = cabeçalho
                String linha = linhas.get(i);
                if (linha.isBlank()) {
                    continue;
                }
                Medicamento m = converterLinha(linha, i + 1);
                cache.put(m.getCodigo(), m);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo " + arquivo + ".", e);
        }
    }

    private Medicamento converterLinha(String linha, int numeroLinha) throws PersistenciaException {
        String[] c = linha.split(SEPARADOR, -1);
        if (c.length != TOTAL_CAMPOS) {
            throw new PersistenciaException("Linha " + numeroLinha + " do CSV inválida: esperados "
                    + TOTAL_CAMPOS + " campos, encontrados " + c.length + ".");
        }
        try {
            Fornecedor f = new Fornecedor(c[8], c[9], c[10], c[11], c[12], c[13]);
            return new Medicamento(c[0], c[1], c[2], c[3], LocalDate.parse(c[4]),
                    Integer.parseInt(c[5]), new BigDecimal(c[6]), Boolean.parseBoolean(c[7]), f);
        } catch (RuntimeException e) {     // DateTimeParseException, NumberFormatException
            throw new PersistenciaException("Linha " + numeroLinha + " do CSV contém valores inválidos ("
                    + e.getMessage() + ").", e);
        }
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    private void persistirOuReverter(Map<String, Medicamento> backup) throws PersistenciaException {
        try {
            List<String> linhas = new ArrayList<>();
            for (Medicamento m : cache.values()) {
                linhas.add(converterMedicamento(m));
            }
            gravar(linhas);
        } catch (IOException e) {
            cache.clear();
            cache.putAll(backup);
            throw new PersistenciaException("Não foi possível gravar o arquivo " + arquivo
                    + ". A operação foi desfeita.", e);
        }
    }

    private void gravar(List<String> linhasDados) throws IOException {
        List<String> todas = new ArrayList<>();
        todas.add(CABECALHO);
        todas.addAll(linhasDados);

        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.write(temporario, todas, StandardCharsets.UTF_8);
        Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
    }

    private String converterMedicamento(Medicamento m) {
        Fornecedor f = m.getFornecedor();
        return String.join(SEPARADOR,
                limpar(m.getCodigo()), limpar(m.getNome()), limpar(m.getDescricao()),
                limpar(m.getPrincipioAtivo()), m.getDataValidade().toString(),
                String.valueOf(m.getQuantidadeEstoque()), m.getPreco().toPlainString(),
                String.valueOf(m.isControlado()),
                limpar(f.getCnpj()), limpar(f.getRazaoSocial()), limpar(f.getTelefone()),
                limpar(f.getEmail()), limpar(f.getCidade()), limpar(f.getEstado()));
    }

    /** Impede que o conteúdo do campo quebre o formato (separador e quebras de linha). */
    private String limpar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(SEPARADOR, ",").replaceAll("[\\r\\n]+", " ").trim();
    }
}
