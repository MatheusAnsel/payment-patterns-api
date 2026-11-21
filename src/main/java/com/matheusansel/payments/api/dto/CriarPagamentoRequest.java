package com.matheusansel.payments.api.dto;

import com.matheusansel.payments.application.NovoPagamentoCommand;
import com.matheusansel.payments.domain.MetodoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarPagamentoRequest(
        @Schema(example = "PED-1001")
        @NotBlank @Size(max = 64)
        String pedidoId,

        @Schema(example = "250.00")
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal valor,

        @Schema(example = "CARTAO_CREDITO")
        @NotNull
        MetodoPagamento metodo,

        @Schema(description = "Apenas para cartão de crédito (1 a 12). Padrão: 1", example = "6")
        @Min(1) @Max(12)
        Integer parcelas
) {

    public NovoPagamentoCommand paraComando() {
        return new NovoPagamentoCommand(pedidoId.trim(), valor, metodo, parcelas);
    }
}
