package com.matheusansel.payments.domain.validation;

import com.matheusansel.payments.domain.MetodoPagamento;

import java.math.BigDecimal;

public record ContextoValidacao(String pedidoId, BigDecimal valor, MetodoPagamento metodo) {
}
