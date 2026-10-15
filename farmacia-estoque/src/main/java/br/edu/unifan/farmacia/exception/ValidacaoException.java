package br.edu.unifan.farmacia.exception;

import java.util.ArrayList;
import java.util.List;

/** Lançada quando os dados informados violam alguma regra de validação. */
public class ValidacaoException extends FarmaciaException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> erros;

    public ValidacaoException(String mensagem) {
        super(mensagem);
        this.erros = new ArrayList<>(List.of(mensagem));
    }

    public ValidacaoException(List<String> erros) {
        super(String.join("\n", erros));
        this.erros = new ArrayList<>(erros);
    }

    public List<String> getErros() {
        return List.copyOf(erros);
    }
}
