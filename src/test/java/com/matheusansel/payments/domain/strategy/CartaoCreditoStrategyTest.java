package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartaoCreditoStrategyTest {

    private final CartaoCreditoStrategy strategy = new CartaoCreditoStrategy();

    @Test
    void atendeOMetodoCartao() {
        assertThat(strategy.metodo()).isEqualTo(MetodoPagamento.CARTAO_CREDITO);
    }

    @Test
    void cobraTaxaDe349PorCentoAVista() {
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("1000.00"), null));

        assertThat(r.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.taxa()).isEqualByComparingTo("34.90");
        assertThat(r.valorFinal()).isEqualByComparingTo("1034.90");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("1034.90");
    }

    @Test
    void arredondaTaxaParaCentavos() {
        // 99,99 * 3,49% = 3,489651 -> 3,49
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("99.99"), 1));
        assertThat(r.taxa()).isEqualTo(new BigDecimal("3.49"));
    }

    @Test
    void ateTresParcelasNaoTemJuros() {
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("1000.00"), 3));

        assertThat(r.valorFinal()).isEqualByComparingTo("1034.90");
        assertThat(r.valorParcela()).isEqualByComparingTo("344.97");
    }

    // Valores esperados calculados de forma independente (Tabela Price, i = 1,99% a.m., sobre valor + taxa)
    @ParameterizedTest
    @CsvSource({
            "1000.00, 6,  34.90, 184.69, 1108.14",
            "1000.00, 12, 34.90, 97.80,  1173.60",
            "500.00,  4,  17.45, 135.86, 543.44"
    })
    void acimaDeTresParcelasAplicaJurosCompostos(String valor, int parcelas, String taxa,
                                                  String valorParcela, String valorFinal) {
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal(valor), parcelas));

        assertThat(r.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.parcelas()).isEqualTo(parcelas);
        assertThat(r.taxa()).isEqualByComparingTo(taxa);
        assertThat(r.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(r.valorFinal()).isEqualByComparingTo(valorFinal);
    }

    @Test
    void aceitaExatamenteOLimiteDeDezMil() {
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("10000.00"), 1));
        assertThat(r.status()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void recusaValorAcimaDeDezMil() {
        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("10000.01"), 1));

        assertThat(r.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(r.motivoRecusa()).contains("10.000,00");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 13})
    void rejeitaQuantidadeDeParcelasForaDoIntervalo(int parcelas) {
        assertThatThrownBy(() -> strategy.processar(new DadosPagamento(new BigDecimal("100.00"), parcelas)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 a 12");
    }
}
