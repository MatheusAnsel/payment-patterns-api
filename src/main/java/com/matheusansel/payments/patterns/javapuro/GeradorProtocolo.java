package com.matheusansel.payments.patterns.javapuro;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton em Java puro, sem Spring, usando o idiom "Initialization-on-demand holder".
 *
 * Por que esse idiom: a JVM so carrega a classe Holder no primeiro acesso a getInstancia(),
 * e o carregamento de classe e thread-safe por especificacao (JLS 12.4.2). Resultado:
 * inicializacao preguicosa e segura sem synchronized nem double-checked locking.
 *
 * Esta classe existe para fins didaticos. Dentro da aplicacao, o papel de Singleton e cumprido
 * pelos beans do Spring, que tem escopo singleton por padrao (uma instancia por ApplicationContext).
 * O singleton "manual" tem custos conhecidos: estado global, dificuldade de substituir em testes
 * e acoplamento ao metodo estatico. Por isso o container de injecao e a escolha preferida.
 */
public final class GeradorProtocolo {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.BASIC_ISO_DATE;

    private final AtomicLong sequencia = new AtomicLong();

    private GeradorProtocolo() {
        if (Holder.INSTANCIA != null) {
            // Bloqueia a criacao de uma segunda instancia via reflection.
            throw new IllegalStateException("Use GeradorProtocolo.getInstancia()");
        }
    }

    private static final class Holder {
        private static final GeradorProtocolo INSTANCIA = new GeradorProtocolo();
    }

    public static GeradorProtocolo getInstancia() {
        return Holder.INSTANCIA;
    }

    /**
     * Gera protocolos no formato PGT-AAAAMMDD-000001, unicos dentro da execucao da JVM.
     */
    public String proximo(LocalDate data) {
        return "PGT-%s-%06d".formatted(data.format(FORMATO_DATA), sequencia.incrementAndGet());
    }
}
