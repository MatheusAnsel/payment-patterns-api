package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.Dinheiro;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Boleto: taxa fixa de R$ 2,50, fica PENDENTE ate a confirmacao e vence em 3 dias uteis.
 * Dias uteis aqui consideram apenas fins de semana; feriados ficam fora do escopo.
 */
@Component
public class BoletoStrategy implements MetodoPagamentoStrategy {

    static final BigDecimal TAXA_FIXA = new BigDecimal("2.50");
    static final int DIAS_UTEIS_VENCIMENTO = 3;

    private final Clock clock;

    public BoletoStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.BOLETO;
    }

    @Override
    public ResultadoProcessamento processar(DadosPagamento dados) {
        ValidacoesComuns.exigirPagamentoAVista(dados, "Boleto");
        BigDecimal valor = Dinheiro.arredondar(dados.valor());
        BigDecimal valorFinal = valor.add(TAXA_FIXA);
        LocalDate vencimento = somarDiasUteis(LocalDate.now(clock), DIAS_UTEIS_VENCIMENTO);
        return new ResultadoProcessamento(StatusPagamento.PENDENTE, TAXA_FIXA, valorFinal, 1, valorFinal,
                vencimento, null);
    }

    static LocalDate somarDiasUteis(LocalDate inicio, int dias) {
        LocalDate data = inicio;
        int adicionados = 0;
        while (adicionados < dias) {
            data = data.plusDays(1);
            if (data.getDayOfWeek() != DayOfWeek.SATURDAY && data.getDayOfWeek() != DayOfWeek.SUNDAY) {
                adicionados++;
            }
        }
        return data;
    }
}
