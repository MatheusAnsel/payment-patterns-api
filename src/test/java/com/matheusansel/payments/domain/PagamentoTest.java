package com.matheusansel.payments.domain;

import com.matheusansel.payments.domain.exception.RegraNegocioException;
import com.matheusansel.payments.domain.strategy.ResultadoProcessamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoTest {

    private static final Instant CRIACAO = Instant.parse("2026-11-18T13:00:00Z");
    private static final Instant DEPOIS = CRIACAO.plusSeconds(60);

    private static Pagamento pagamento(MetodoPagamento metodo, StatusPagamento status) {
        return Pagamento.criar("PED-1", new BigDecimal("100"), metodo,
                ResultadoProcessamento.aVista(status, BigDecimal.ZERO, new BigDecimal("100.00")), CRIACAO);
    }

    @Test
    void naoPermiteValorZeroOuNegativo() {
        ResultadoProcessamento r = ResultadoProcessamento.aVista(StatusPagamento.APROVADO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThatThrownBy(() -> Pagamento.criar("PED-1", BigDecimal.ZERO, MetodoPagamento.PIX, r, CRIACAO))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> Pagamento.criar("PED-1", new BigDecimal("-1"), MetodoPagamento.PIX, r, CRIACAO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void confirmaBoletoPendente() {
        Pagamento boleto = pagamento(MetodoPagamento.BOLETO, StatusPagamento.PENDENTE);

        boleto.confirmar(DEPOIS);

        assertThat(boleto.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(boleto.getAtualizadoEm()).isEqualTo(DEPOIS);
        assertThat(boleto.getCriadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void naoConfirmaPagamentoQueNaoEBoleto() {
        Pagamento pix = pagamento(MetodoPagamento.PIX, StatusPagamento.APROVADO);
        assertThatThrownBy(() -> pix.confirmar(DEPOIS)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("boleto");
    }

    @Test
    void naoConfirmaBoletoJaAprovado() {
        Pagamento boleto = pagamento(MetodoPagamento.BOLETO, StatusPagamento.PENDENTE);
        boleto.confirmar(DEPOIS);
        assertThatThrownBy(() -> boleto.confirmar(DEPOIS)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void estornaPagamentoAprovado() {
        Pagamento pix = pagamento(MetodoPagamento.PIX, StatusPagamento.APROVADO);

        pix.estornar(DEPOIS);

        assertThat(pix.getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
    }

    @Test
    void naoEstornaPagamentoNaoAprovado() {
        assertThatThrownBy(() -> pagamento(MetodoPagamento.BOLETO, StatusPagamento.PENDENTE).estornar(DEPOIS))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> pagamento(MetodoPagamento.CARTAO_CREDITO, StatusPagamento.RECUSADO).estornar(DEPOIS))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoEstornaDuasVezes() {
        Pagamento pix = pagamento(MetodoPagamento.PIX, StatusPagamento.APROVADO);
        pix.estornar(DEPOIS);
        assertThatThrownBy(() -> pix.estornar(DEPOIS)).isInstanceOf(RegraNegocioException.class);
    }
}
