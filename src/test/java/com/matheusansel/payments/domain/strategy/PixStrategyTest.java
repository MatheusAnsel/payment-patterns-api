package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PixStrategyTest {

    private final PixStrategy strategy = new PixStrategy();

    @Test
    void atendeOMetodoPix() {
        assertThat(strategy.metodo()).isEqualTo(MetodoPagamento.PIX);
    }

    @Test
    void aprovaNaHoraESemTaxa() {
        ResultadoProcessamento resultado = strategy.processar(new DadosPagamento(new BigDecimal("150.5"), null));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.taxa()).isEqualByComparingTo("0");
        assertThat(resultado.valorFinal()).isEqualTo(new BigDecimal("150.50"));
        assertThat(resultado.parcelas()).isEqualTo(1);
        assertThat(resultado.dataVencimento()).isNull();
    }

    @Test
    void aceitaParcelaUnicaInformadaExplicitamente() {
        assertThat(strategy.processar(new DadosPagamento(BigDecimal.TEN, 1)).status())
                .isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void naoAceitaParcelamento() {
        assertThatThrownBy(() -> strategy.processar(new DadosPagamento(BigDecimal.TEN, 2)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("parcelamento");
    }
}
