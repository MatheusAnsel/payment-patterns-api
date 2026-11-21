package com.matheusansel.payments.domain.validation;

import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;

/**
 * Madrugada concentra boa parte das fraudes. Entre 00:00 e 05:59 (horario de Brasilia),
 * pagamentos acima de R$ 1.000,00 sao bloqueados.
 */
public class HorarioValidador extends ValidadorAntifraude {

    public static final BigDecimal LIMITE_MADRUGADA = new BigDecimal("1000.00");
    private static final LocalTime FIM_MADRUGADA = LocalTime.of(6, 0);

    private final Clock clock;

    public HorarioValidador(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void verificar(ContextoValidacao contexto) {
        LocalTime agora = LocalTime.now(clock);
        boolean madrugada = agora.isBefore(FIM_MADRUGADA);
        if (madrugada && contexto.valor().compareTo(LIMITE_MADRUGADA) > 0) {
            throw new PagamentoBloqueadoException("HORARIO",
                    "Pagamentos acima de R$ 1.000,00 nao sao permitidos entre 00:00 e 06:00");
        }
    }
}
