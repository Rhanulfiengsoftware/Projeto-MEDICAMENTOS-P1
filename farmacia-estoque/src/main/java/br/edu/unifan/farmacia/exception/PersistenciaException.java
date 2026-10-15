package br.edu.unifan.farmacia.exception;

/** Lançada em falhas de leitura/escrita do arquivo CSV. */
public class PersistenciaException extends FarmaciaException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
