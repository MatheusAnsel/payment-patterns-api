package com.matheusansel.payments.domain.validation;

/**
 * Elo da cadeia antifraude (Chain of Responsibility). Cada elo verifica uma regra e,
 * se estiver tudo certo, repassa para o proximo. O primeiro elo que reprovar interrompe a cadeia
 * lancando {@link com.matheusansel.payments.domain.exception.PagamentoBloqueadoException}.
 */
public abstract class ValidadorAntifraude {

    private ValidadorAntifraude proximo;

    /**
     * Encadeia o proximo elo e o devolve, permitindo a.seguir(b).seguir(c).
     */
    public ValidadorAntifraude seguir(ValidadorAntifraude proximo) {
        this.proximo = proximo;
        return proximo;
    }

    public final void validar(ContextoValidacao contexto) {
        verificar(contexto);
        if (proximo != null) {
            proximo.validar(contexto);
        }
    }

    protected abstract void verificar(ContextoValidacao contexto);
}
