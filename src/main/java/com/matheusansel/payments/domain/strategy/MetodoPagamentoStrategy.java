package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;

/**
 * Cada metodo de pagamento tem regras proprias de taxa, validacao e status inicial.
 * Adicionar um metodo novo significa criar uma nova implementacao, sem mexer nas existentes.
 */
public interface MetodoPagamentoStrategy {

    MetodoPagamento metodo();

    ResultadoProcessamento processar(DadosPagamento dados);
}
