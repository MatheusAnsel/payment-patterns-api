package com.matheusansel.payments.application;

import com.matheusansel.payments.domain.MetodoPagamento;

import java.math.BigDecimal;

public record NovoPagamentoCommand(String pedidoId, BigDecimal valor, MetodoPagamento metodo, Integer parcelas) {
}
