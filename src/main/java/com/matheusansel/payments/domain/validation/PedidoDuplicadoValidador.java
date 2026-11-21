package com.matheusansel.payments.domain.validation;

import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;

import java.util.EnumSet;

/**
 * Impede cobrar duas vezes o mesmo pedido. Pagamentos recusados ou estornados
 * liberam o pedido para uma nova tentativa.
 */
public class PedidoDuplicadoValidador extends ValidadorAntifraude {

    private static final EnumSet<StatusPagamento> STATUS_ATIVOS =
            EnumSet.of(StatusPagamento.PENDENTE, StatusPagamento.APROVADO);

    private final PagamentoRepository repository;

    public PedidoDuplicadoValidador(PagamentoRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void verificar(ContextoValidacao contexto) {
        if (repository.existsByPedidoIdAndStatusIn(contexto.pedidoId(), STATUS_ATIVOS)) {
            throw new PagamentoBloqueadoException("PEDIDO_DUPLICADO",
                    "Ja existe um pagamento pendente ou aprovado para o pedido " + contexto.pedidoId());
        }
    }
}
