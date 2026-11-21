package com.matheusansel.payments.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de arredondamento monetario centralizadas para nao espalhar
 * setScale(2, HALF_UP) pelo codigo.
 */
public final class Dinheiro {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA, ARREDONDAMENTO);
    }
}
