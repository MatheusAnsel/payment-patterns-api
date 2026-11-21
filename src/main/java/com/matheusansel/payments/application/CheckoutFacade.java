package com.matheusansel.payments.application;

import com.matheusansel.payments.application.evento.PagamentoAprovadoEvent;
import com.matheusansel.payments.application.evento.PagamentoEstornadoEvent;
import com.matheusansel.payments.application.evento.PagamentoRecusadoEvent;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.PagamentoNaoEncontradoException;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import com.matheusansel.payments.domain.strategy.DadosPagamento;
import com.matheusansel.payments.domain.strategy.PagamentoStrategyFactory;
import com.matheusansel.payments.domain.strategy.ResultadoProcessamento;
import com.matheusansel.payments.domain.validation.ContextoValidacao;
import com.matheusansel.payments.domain.validation.ValidadorAntifraude;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;
import com.matheusansel.payments.infrastructure.repository.PagamentoSpecifications;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Ponto unico de entrada do fluxo de pagamento. O controller nao conhece strategies,
 * cadeia antifraude, repositorio nem eventos: tudo isso e orquestrado aqui.
 */
@Service
public class CheckoutFacade {

    private final ValidadorAntifraude cadeiaAntifraude;
    private final PagamentoStrategyFactory strategyFactory;
    private final PagamentoRepository repository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public CheckoutFacade(ValidadorAntifraude cadeiaAntifraude, PagamentoStrategyFactory strategyFactory,
                          PagamentoRepository repository, ApplicationEventPublisher eventos, Clock clock) {
        this.cadeiaAntifraude = cadeiaAntifraude;
        this.strategyFactory = strategyFactory;
        this.repository = repository;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public Pagamento processar(NovoPagamentoCommand comando) {
        if (comando.valor() == null || comando.valor().signum() <= 0) {
            throw new RegraNegocioException("O valor do pagamento deve ser maior que zero");
        }
        cadeiaAntifraude.validar(new ContextoValidacao(comando.pedidoId(), comando.valor(), comando.metodo()));

        ResultadoProcessamento resultado = strategyFactory.obter(comando.metodo())
                .processar(new DadosPagamento(comando.valor(), comando.parcelas()));

        Pagamento pagamento = repository.save(
                Pagamento.criar(comando.pedidoId(), comando.valor(), comando.metodo(), resultado, agora()));

        publicarResultado(pagamento);
        return pagamento;
    }

    @Transactional(readOnly = true)
    public Pagamento buscar(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PagamentoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Page<Pagamento> listar(StatusPagamento status, MetodoPagamento metodo, Pageable pageable) {
        return repository.findAll(PagamentoSpecifications.comFiltros(status, metodo), pageable);
    }

    @Transactional
    public Pagamento confirmarBoleto(UUID id) {
        Pagamento pagamento = buscar(id);
        pagamento.confirmar(agora());
        publicarResultado(pagamento);
        return pagamento;
    }

    @Transactional
    public Pagamento estornar(UUID id) {
        Pagamento pagamento = buscar(id);
        pagamento.estornar(agora());
        eventos.publishEvent(new PagamentoEstornadoEvent(pagamento.getId(), pagamento.getPedidoId(),
                pagamento.getValorFinal(), pagamento.getAtualizadoEm()));
        return pagamento;
    }

    private void publicarResultado(Pagamento pagamento) {
        switch (pagamento.getStatus()) {
            case APROVADO -> eventos.publishEvent(new PagamentoAprovadoEvent(pagamento.getId(),
                    pagamento.getPedidoId(), pagamento.getMetodo(), pagamento.getValorFinal(),
                    pagamento.getAtualizadoEm()));
            case RECUSADO -> eventos.publishEvent(new PagamentoRecusadoEvent(pagamento.getId(),
                    pagamento.getPedidoId(), pagamento.getMetodo(), pagamento.getValor(),
                    pagamento.getMotivoRecusa(), pagamento.getAtualizadoEm()));
            default -> {
                // PENDENTE nao gera evento: o boleto ainda depende de compensacao
            }
        }
    }

    /**
     * Truncado em microssegundos, a precisao do TIMESTAMP do PostgreSQL. Sem isso a resposta do POST
     * traria nanossegundos e um GET logo depois devolveria um valor diferente para o mesmo campo.
     */
    private Instant agora() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
