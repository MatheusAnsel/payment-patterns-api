package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.MetodoPagamento;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve a strategy pelo metodo de pagamento. O Spring injeta todas as implementacoes de
 * {@link MetodoPagamentoStrategy}; a factory monta o mapa uma vez e falha na inicializacao se
 * houver metodo sem strategy ou strategy duplicada, em vez de descobrir isso em producao.
 */
@Component
public class PagamentoStrategyFactory {

    private final Map<MetodoPagamento, MetodoPagamentoStrategy> strategies;

    public PagamentoStrategyFactory(List<MetodoPagamentoStrategy> implementacoes) {
        Map<MetodoPagamento, MetodoPagamentoStrategy> mapa = new EnumMap<>(MetodoPagamento.class);
        for (MetodoPagamentoStrategy strategy : implementacoes) {
            MetodoPagamentoStrategy anterior = mapa.put(strategy.metodo(), strategy);
            if (anterior != null) {
                throw new IllegalStateException("Mais de uma strategy para " + strategy.metodo() + ": "
                        + anterior.getClass().getSimpleName() + " e " + strategy.getClass().getSimpleName());
            }
        }
        List<MetodoPagamento> semStrategy = Arrays.stream(MetodoPagamento.values())
                .filter(m -> !mapa.containsKey(m))
                .toList();
        if (!semStrategy.isEmpty()) {
            throw new IllegalStateException("Metodos de pagamento sem strategy: " + semStrategy);
        }
        this.strategies = Map.copyOf(mapa);
    }

    public MetodoPagamentoStrategy obter(MetodoPagamento metodo) {
        return strategies.get(metodo);
    }
}
