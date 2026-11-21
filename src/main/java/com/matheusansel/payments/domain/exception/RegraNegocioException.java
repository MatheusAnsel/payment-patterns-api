package com.matheusansel.payments.domain.exception;

/**
 * Violacao de regra de negocio. A API traduz para HTTP 422.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
