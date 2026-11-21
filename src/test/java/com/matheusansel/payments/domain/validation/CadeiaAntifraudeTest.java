package com.matheusansel.payments.domain.validation;

import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;
import com.matheusansel.payments.infrastructure.config.AntifraudeConfig;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadeiaAntifraudeTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Mock
    private PagamentoRepository repository;

    private ValidadorAntifraude cadeiaAs(int hora, int minuto) {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 18, hora, minuto).atZone(SP).toInstant(), SP);
        return new AntifraudeConfig().cadeiaAntifraude(repository, clock);
    }

    private static ContextoValidacao contexto(String valor) {
        return new ContextoValidacao("PED-1", new BigDecimal(valor), MetodoPagamento.PIX);
    }

    @Test
    void deixaPassarPagamentoRegular() {
        assertThatCode(() -> cadeiaAs(14, 0).validar(contexto("500.00"))).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaValorAcimaDoMaximoSemConsultarOBanco() {
        assertThatThrownBy(() -> cadeiaAs(14, 0).validar(contexto("50000.01")))
                .isInstanceOf(PagamentoBloqueadoException.class)
                .extracting("regra").isEqualTo("VALOR_MAXIMO");

        // o primeiro elo interrompe a cadeia: a regra que acessa o banco nem roda
        verifyNoInteractions(repository);
    }

    @Test
    void aceitaExatamenteOValorMaximo() {
        assertThatCode(() -> cadeiaAs(14, 0).validar(contexto("50000.00"))).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaValorAltoDeMadrugada() {
        assertThatThrownBy(() -> cadeiaAs(3, 30).validar(contexto("1000.01")))
                .isInstanceOf(PagamentoBloqueadoException.class)
                .extracting("regra").isEqualTo("HORARIO");
        verifyNoInteractions(repository);
    }

    @Test
    void permiteValorBaixoDeMadrugada() {
        assertThatCode(() -> cadeiaAs(3, 30).validar(contexto("1000.00"))).doesNotThrowAnyException();
    }

    @Test
    void madrugadaTerminaAsSeisHoras() {
        assertThatThrownBy(() -> cadeiaAs(5, 59).validar(contexto("2000.00")))
                .isInstanceOf(PagamentoBloqueadoException.class);
        assertThatCode(() -> cadeiaAs(6, 0).validar(contexto("2000.00"))).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaPedidoComPagamentoAtivo() {
        when(repository.existsByPedidoIdAndStatusIn(eq("PED-1"), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> cadeiaAs(14, 0).validar(contexto("100.00")))
                .isInstanceOf(PagamentoBloqueadoException.class)
                .extracting("regra").isEqualTo("PEDIDO_DUPLICADO");
    }

    @Test
    void elosPodemSerEncadeadosEmQualquerComposicao() {
        when(repository.existsByPedidoIdAndStatusIn(any(), anyCollection())).thenReturn(true);
        ValidadorAntifraude soDuplicidade = new PedidoDuplicadoValidador(repository);

        assertThatThrownBy(() -> soDuplicidade.validar(contexto("999999.00")))
                .extracting("regra").isEqualTo("PEDIDO_DUPLICADO");
    }
}
