package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoStrategyFactoryTest {

    private final PixStrategy pix = new PixStrategy();
    private final CartaoCreditoStrategy cartao = new CartaoCreditoStrategy();
    private final BoletoStrategy boleto = new BoletoStrategy(Clock.systemUTC());

    @ParameterizedTest
    @EnumSource(MetodoPagamento.class)
    void resolveUmaStrategyParaCadaMetodo(MetodoPagamento metodo) {
        PagamentoStrategyFactory factory = new PagamentoStrategyFactory(List.of(pix, cartao, boleto));

        assertThat(factory.obter(metodo).metodo()).isEqualTo(metodo);
    }

    @Test
    void devolveAImplementacaoCorreta() {
        PagamentoStrategyFactory factory = new PagamentoStrategyFactory(List.of(boleto, pix, cartao));

        assertThat(factory.obter(MetodoPagamento.PIX)).isSameAs(pix);
        assertThat(factory.obter(MetodoPagamento.CARTAO_CREDITO)).isSameAs(cartao);
        assertThat(factory.obter(MetodoPagamento.BOLETO)).isSameAs(boleto);
    }

    @Test
    void falhaNaInicializacaoSeFaltarStrategy() {
        assertThatThrownBy(() -> new PagamentoStrategyFactory(List.of(pix, cartao)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOLETO");
    }

    @Test
    void falhaNaInicializacaoSeHouverStrategyDuplicada() {
        assertThatThrownBy(() -> new PagamentoStrategyFactory(List.of(pix, cartao, boleto, new PixStrategy())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mais de uma strategy para PIX");
    }
}
