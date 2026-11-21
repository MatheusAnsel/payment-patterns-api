package com.matheusansel.payments.application;

import com.matheusansel.payments.application.evento.PagamentoAprovadoEvent;
import com.matheusansel.payments.application.evento.PagamentoEstornadoEvent;
import com.matheusansel.payments.application.evento.PagamentoRecusadoEvent;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;
import com.matheusansel.payments.domain.exception.PagamentoNaoEncontradoException;
import com.matheusansel.payments.domain.strategy.BoletoStrategy;
import com.matheusansel.payments.domain.strategy.CartaoCreditoStrategy;
import com.matheusansel.payments.domain.strategy.PagamentoStrategyFactory;
import com.matheusansel.payments.domain.strategy.PixStrategy;
import com.matheusansel.payments.domain.validation.ValidadorAntifraude;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutFacadeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T17:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock
    private ValidadorAntifraude antifraude;
    @Mock
    private PagamentoRepository repository;
    @Mock
    private ApplicationEventPublisher eventos;

    private CheckoutFacade facade;

    @BeforeEach
    void setUp() {
        PagamentoStrategyFactory factory = new PagamentoStrategyFactory(
                java.util.List.of(new PixStrategy(), new CartaoCreditoStrategy(), new BoletoStrategy(CLOCK)));
        facade = new CheckoutFacade(antifraude, factory, repository, eventos, CLOCK);
    }

    private void repositorioDevolveOQueSalva() {
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void pixAprovadoEPersistidoEPublicaEventoDeAprovacao() {
        repositorioDevolveOQueSalva();

        Pagamento p = facade.processar(new NovoPagamentoCommand("PED-1", new BigDecimal("80.00"), MetodoPagamento.PIX, null));

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(p.getCriadoEm()).isEqualTo(CLOCK.instant());
        verify(antifraude).validar(any());
        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue()).isInstanceOfSatisfying(PagamentoAprovadoEvent.class,
                e -> assertThat(e.pedidoId()).isEqualTo("PED-1"));
    }

    @Test
    void cartaoAcimaDoLimiteEPersistidoComoRecusadoEPublicaEventoDeRecusa() {
        repositorioDevolveOQueSalva();

        Pagamento p = facade.processar(new NovoPagamentoCommand("PED-2", new BigDecimal("15000.00"),
                MetodoPagamento.CARTAO_CREDITO, 1));

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        verify(eventos).publishEvent(any(PagamentoRecusadoEvent.class));
    }

    @Test
    void boletoPendenteNaoPublicaEvento() {
        repositorioDevolveOQueSalva();

        Pagamento p = facade.processar(new NovoPagamentoCommand("PED-3", new BigDecimal("80.00"), MetodoPagamento.BOLETO, null));

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        verifyNoInteractions(eventos);
    }

    @Test
    void bloqueioDoAntifraudeInterrompeOFluxoAntesDePersistir() {
        doThrow(new PagamentoBloqueadoException("VALOR_MAXIMO", "bloqueado")).when(antifraude).validar(any());

        assertThatThrownBy(() -> facade.processar(new NovoPagamentoCommand("PED-4", new BigDecimal("80.00"),
                MetodoPagamento.PIX, null))).isInstanceOf(PagamentoBloqueadoException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void confirmarBoletoAprovaEPublicaEvento() {
        repositorioDevolveOQueSalva();
        Pagamento boleto = facade.processar(new NovoPagamentoCommand("PED-5", BigDecimal.TEN, MetodoPagamento.BOLETO, null));
        when(repository.findById(boleto.getId())).thenReturn(Optional.of(boleto));

        facade.confirmarBoleto(boleto.getId());

        assertThat(boleto.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(eventos).publishEvent(any(PagamentoAprovadoEvent.class));
    }

    @Test
    void estornarPublicaEventoDeEstorno() {
        repositorioDevolveOQueSalva();
        Pagamento pix = facade.processar(new NovoPagamentoCommand("PED-6", BigDecimal.TEN, MetodoPagamento.PIX, null));
        when(repository.findById(pix.getId())).thenReturn(Optional.of(pix));

        facade.estornar(pix.getId());

        assertThat(pix.getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        verify(eventos).publishEvent(any(PagamentoEstornadoEvent.class));
    }

    @Test
    void buscarPagamentoInexistenteLancaNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.buscar(id)).isInstanceOf(PagamentoNaoEncontradoException.class);
    }
}
