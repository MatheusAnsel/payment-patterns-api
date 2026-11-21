package com.matheusansel.payments.domain.validation;

import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;

import java.math.BigDecimal;

/**
 * Teto absoluto por transacao, independente do metodo. Valores acima disso exigem
 * analise manual e nem chegam a ser processados.
 */
public class ValorMaximoValidador extends ValidadorAntifraude {

    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("50000.00");

    @Override
    protected void verificar(ContextoValidacao contexto) {
        if (contexto.valor().compareTo(VALOR_MAXIMO) > 0) {
            throw new PagamentoBloqueadoException("VALOR_MAXIMO",
                    "Valor acima do máximo permitido por transação (R$ 50.000,00)");
        }
    }
}
