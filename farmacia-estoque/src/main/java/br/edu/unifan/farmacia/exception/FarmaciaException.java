package br.edu.unifan.farmacia.exception;

/** Exceção base (checada) de todas as falhas de negócio do sistema. */
public class FarmaciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public FarmaciaException(String mensagem) {
        super(mensagem);
    }

    public FarmaciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
