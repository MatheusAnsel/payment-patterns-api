package com.matheusansel.payments.domain.exception;

/**
 * Lancada pela cadeia antifraude quando um pagamento nao pode nem ser processado.
 */
public class PagamentoBloqueadoException extends RegraNegocioException {

    private final String regra;

    public PagamentoBloqueadoException(String regra, String mensagem) {
        super(mensagem);
        this.regra = regra;
    }

    public String getRegra() {
        return regra;
    }
}
