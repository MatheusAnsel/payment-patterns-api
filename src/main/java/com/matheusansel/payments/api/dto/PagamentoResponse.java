package com.matheusansel.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagamentoResponse(
        UUID id,
        String pedidoId,
        BigDecimal valor,
        MetodoPagamento metodo,
        StatusPagamento status,
        BigDecimal taxa,
        BigDecimal valorFinal,
        int parcelas,
        BigDecimal valorParcela,
        LocalDate dataVencimento,
        String motivoRecusa,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static PagamentoResponse de(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getPedidoId(), p.getValor(), p.getMetodo(), p.getStatus(),
                p.getTaxa(), p.getValorFinal(), p.getParcelas(), p.getValorParcela(), p.getDataVencimento(),
                p.getMotivoRecusa(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
