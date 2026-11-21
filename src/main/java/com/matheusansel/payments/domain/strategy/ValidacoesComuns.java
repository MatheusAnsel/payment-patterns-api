package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.exception.RegraNegocioException;

final class ValidacoesComuns {

    private ValidacoesComuns() {
    }

    static void exigirPagamentoAVista(DadosPagamento dados, String nomeMetodo) {
        if (dados.parcelas() != null && dados.parcelas() != 1) {
            throw new RegraNegocioException(nomeMetodo + " não aceita parcelamento");
        }
    }
}
