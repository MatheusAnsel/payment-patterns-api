package com.matheusansel.payments.domain.exception;

import java.util.UUID;

public class PagamentoNaoEncontradoException extends RuntimeException {

    public PagamentoNaoEncontradoException(UUID id) {
        super("Pagamento " + id + " nao encontrado");
    }
}
