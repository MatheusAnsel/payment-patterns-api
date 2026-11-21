package com.matheusansel.payments.application.evento;

import com.matheusansel.payments.domain.MetodoPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagamentoRecusadoEvent(UUID pagamentoId, String pedidoId, MetodoPagamento metodo,
                                     BigDecimal valor, String motivo, Instant ocorridoEm) {
}
