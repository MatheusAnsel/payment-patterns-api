package com.matheusansel.payments.domain.strategy;

import com.matheusansel.payments.domain.Dinheiro;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cartao de credito:
 * - taxa de 3,49% sobre o valor;
 * - de 1 a 12 parcelas; ate 3 parcelas sem juros;
 * - acima de 3 parcelas, juros compostos de 1,99% a.m. pela Tabela Price sobre (valor + taxa);
 * - valores acima de R$ 10.000,00 sao recusados (o pagamento e registrado como RECUSADO).
 */
@Component
public class CartaoCreditoStrategy implements MetodoPagamentoStrategy {

    static final BigDecimal PERCENTUAL_TAXA = new BigDecimal("0.0349");
    static final BigDecimal JUROS_MENSAL = new BigDecimal("0.0199");
    static final BigDecimal LIMITE = new BigDecimal("10000.00");
    static final int MAX_PARCELAS = 12;
    static final int PARCELAS_SEM_JUROS = 3;

    private static final MathContext PRECISAO = MathContext.DECIMAL64;

    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.CARTAO_CREDITO;
    }

    @Override
    public ResultadoProcessamento processar(DadosPagamento dados) {
        int parcelas = dados.parcelas() == null ? 1 : dados.parcelas();
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new RegraNegocioException("Cartao de credito aceita de 1 a " + MAX_PARCELAS + " parcelas");
        }

        BigDecimal valor = Dinheiro.arredondar(dados.valor());
        BigDecimal taxa = Dinheiro.arredondar(valor.multiply(PERCENTUAL_TAXA));
        BigDecimal base = valor.add(taxa);

        BigDecimal valorParcela = parcelas <= PARCELAS_SEM_JUROS
                ? Dinheiro.arredondar(base.divide(BigDecimal.valueOf(parcelas), PRECISAO))
                : Dinheiro.arredondar(parcelaPrice(base, parcelas));
        // Sem juros, o total e exatamente valor + taxa; a ultima parcela absorve a diferenca de centavos.
        BigDecimal valorFinal = parcelas <= PARCELAS_SEM_JUROS
                ? base
                : valorParcela.multiply(BigDecimal.valueOf(parcelas));

        if (valor.compareTo(LIMITE) > 0) {
            return new ResultadoProcessamento(StatusPagamento.RECUSADO, taxa, valorFinal, parcelas, valorParcela,
                    null, "Valor acima do limite de R$ 10.000,00 para cartao de credito");
        }
        return new ResultadoProcessamento(StatusPagamento.APROVADO, taxa, valorFinal, parcelas, valorParcela,
                null, null);
    }

    /**
     * PMT = PV * i / (1 - (1 + i)^-n)
     */
    private BigDecimal parcelaPrice(BigDecimal principal, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(JUROS_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal descontado = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return principal.multiply(JUROS_MENSAL).divide(descontado, PRECISAO);
    }
}
