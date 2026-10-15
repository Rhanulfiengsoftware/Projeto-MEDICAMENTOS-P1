package br.edu.unifan.farmacia.service;

import br.edu.unifan.farmacia.exception.PersistenciaException;
import br.edu.unifan.farmacia.exception.ValidacaoException;
import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;
import br.edu.unifan.farmacia.repository.MedicamentoRepository;
import br.edu.unifan.farmacia.validation.Validador;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio das operações básicas: cadastrar, excluir, consultar
 * por código e listar.
 */
public class MedicamentoService {

    private final MedicamentoRepository repositorio;

    public MedicamentoService(MedicamentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void cadastrar(Medicamento medicamento) throws ValidacaoException, PersistenciaException {
        normalizar(medicamento);

        List<String> erros = new ArrayList<>(Validador.validar(medicamento, LocalDate.now()));
        if (medicamento.getCodigo() != null && repositorio.existe(medicamento.getCodigo())) {
            erros.add("Já existe um medicamento cadastrado com o código " + medicamento.getCodigo() + ".");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        repositorio.salvar(medicamento);
    }

    public void excluir(String codigo) throws ValidacaoException, PersistenciaException {
        String chave = codigo == null ? "" : codigo.trim().toUpperCase();
        if (chave.isEmpty()) {
            throw new ValidacaoException("Informe o código do medicamento.");
        }
        if (!repositorio.remover(chave)) {
            throw new ValidacaoException("Medicamento " + chave + " não encontrado.");
        }
    }

    public Optional<Medicamento> consultar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return repositorio.buscarPorCodigo(codigo.trim().toUpperCase());
    }

    /** Lista todos os medicamentos ordenados por nome. */
    public List<Medicamento> listar() {
        return repositorio.listarTodos().stream()
                .sorted(Comparator.comparing(Medicamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Padroniza os dados antes de validar/gravar (trim, caixa alta, apenas dígitos). */
    private void normalizar(Medicamento m) {
        m.setCodigo(trimUpper(m.getCodigo()));
        m.setNome(trim(m.getNome()));
        m.setDescricao(trim(m.getDescricao()));
        m.setPrincipioAtivo(trim(m.getPrincipioAtivo()));
        if (m.getPreco() != null) {
            m.setPreco(m.getPreco().setScale(2, RoundingMode.HALF_UP));
        }
        Fornecedor f = m.getFornecedor();
        if (f != null) {
            f.setCnpj(Validador.somenteDigitos(f.getCnpj()));
            f.setRazaoSocial(trim(f.getRazaoSocial()));
            f.setTelefone(Validador.somenteDigitos(f.getTelefone()));
            f.setEmail(trim(f.getEmail()));
            f.setCidade(trim(f.getCidade()));
            f.setEstado(trimUpper(f.getEstado()));
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String trimUpper(String s) {
        return trim(s).toUpperCase();
    }
}
