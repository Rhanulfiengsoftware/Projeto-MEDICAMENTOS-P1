package br.edu.unifan.farmacia.repository;

import br.edu.unifan.farmacia.exception.PersistenciaException;
import br.edu.unifan.farmacia.model.Medicamento;

import java.util.List;
import java.util.Optional;

/** Contrato de persistência de medicamentos (desacopla serviço e armazenamento). */
public interface MedicamentoRepository {

    boolean existe(String codigo);

    void salvar(Medicamento medicamento) throws PersistenciaException;

    /** @return {@code true} se o medicamento existia e foi removido. */
    boolean remover(String codigo) throws PersistenciaException;

    Optional<Medicamento> buscarPorCodigo(String codigo);

    List<Medicamento> listarTodos();
}
