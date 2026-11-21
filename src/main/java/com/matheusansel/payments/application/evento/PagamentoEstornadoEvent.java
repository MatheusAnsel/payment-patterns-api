package com.matheusansel.payments.application.evento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagamentoEstornadoEvent(UUID pagamentoId, String pedidoId, BigDecimal valorFinal,
                                      Instant ocorridoEm) {
}
