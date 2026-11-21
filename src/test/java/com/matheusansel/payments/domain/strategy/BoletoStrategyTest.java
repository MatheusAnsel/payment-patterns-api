package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoletoStrategyTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private BoletoStrategy strategyEm(LocalDate data) {
        return new BoletoStrategy(Clock.fixed(ZonedDateTime.of(data.atTime(10, 0), SP).toInstant(), SP));
    }

    @Test
    void cobraTaxaFixaEFicaPendente() {
        BoletoStrategy strategy = strategyEm(LocalDate.of(2026, 11, 16));

        ResultadoProcessamento r = strategy.processar(new DadosPagamento(new BigDecimal("100.00"), null));

        assertThat(strategy.metodo()).isEqualTo(MetodoPagamento.BOLETO);
        assertThat(r.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(r.taxa()).isEqualByComparingTo("2.50");
        assertThat(r.valorFinal()).isEqualByComparingTo("102.50");
    }

    @ParameterizedTest(name = "emitido em {0} vence em {1}")
    @CsvSource({
            "2026-11-16, 2026-11-19", // segunda -> quinta
            "2026-11-18, 2026-11-23", // quarta  -> segunda (pula o fim de semana)
            "2026-11-20, 2026-11-25", // sexta   -> quarta
            "2026-11-21, 2026-11-25", // sabado  -> quarta
            "2026-11-22, 2026-11-25"  // domingo -> quarta
    })
    void venceEmTresDiasUteis(LocalDate emissao, LocalDate vencimentoEsperado) {
        ResultadoProcessamento r = strategyEm(emissao).processar(new DadosPagamento(BigDecimal.TEN, null));
        assertThat(r.dataVencimento()).isEqualTo(vencimentoEsperado);
    }

    @Test
    void naoAceitaParcelamento() {
        BoletoStrategy strategy = strategyEm(LocalDate.of(2026, 11, 16));
        assertThatThrownBy(() -> strategy.processar(new DadosPagamento(BigDecimal.TEN, 3)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
