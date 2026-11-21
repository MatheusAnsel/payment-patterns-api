package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.Dinheiro;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pix: sem taxa e com aprovacao imediata.
 */
@Component
public class PixStrategy implements MetodoPagamentoStrategy {

    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.PIX;
    }

    @Override
    public ResultadoProcessamento processar(DadosPagamento dados) {
        ValidacoesComuns.exigirPagamentoAVista(dados, "Pix");
        BigDecimal valor = Dinheiro.arredondar(dados.valor());
        return ResultadoProcessamento.aVista(StatusPagamento.APROVADO, Dinheiro.arredondar(BigDecimal.ZERO), valor);
    }
}
