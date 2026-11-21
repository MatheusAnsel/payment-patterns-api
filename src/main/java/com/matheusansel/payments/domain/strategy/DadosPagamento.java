package com.matheusansel.payments.domain.strategy;

import java.math.BigDecimal;

/**
 * Entrada de uma strategy. Parcelas pode ser nulo quando o cliente nao informa.
 */
public record DadosPagamento(BigDecimal valor, Integer parcelas) {
}
