package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que uma strategy decide sobre o pagamento: custos, status inicial e dados especificos do metodo.
 */
public record ResultadoProcessamento(
        StatusPagamento status,
        BigDecimal taxa,
        BigDecimal valorFinal,
        int parcelas,
        BigDecimal valorParcela,
        LocalDate dataVencimento,
        String motivoRecusa
) {

    public static ResultadoProcessamento aVista(StatusPagamento status, BigDecimal taxa, BigDecimal valorFinal) {
        return new ResultadoProcessamento(status, taxa, valorFinal, 1, valorFinal, null, null);
    }
}
