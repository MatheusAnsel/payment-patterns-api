package com.matheusansel.payments.patterns.javapuro;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeradorProtocoloTest {

    private static final LocalDate DATA = LocalDate.of(2026, 11, 21);

    @Test
    void sempreDevolveAMesmaInstancia() {
        assertThat(GeradorProtocolo.getInstancia()).isSameAs(GeradorProtocolo.getInstancia());
    }

    @Test
    void geraProtocoloNoFormatoEsperado() {
        assertThat(GeradorProtocolo.getInstancia().proximo(DATA)).matches("PGT-20261121-\\d{6}");
    }

    @Test
    void instanciaUnicaECompartilhadaEntreThreads() throws InterruptedException {
        Set<GeradorProtocolo> instancias = ConcurrentHashMap.newKeySet();
        Set<String> protocolos = ConcurrentHashMap.newKeySet();
        int chamadas = 1_000;

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            IntStream.range(0, chamadas).forEach(i -> executor.submit(() -> {
                GeradorProtocolo gerador = GeradorProtocolo.getInstancia();
                instancias.add(gerador);
                protocolos.add(gerador.proximo(DATA));
            }));
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(instancias).hasSize(1);
        assertThat(protocolos).hasSize(chamadas); // sequencia atomica: nenhum protocolo repetido
    }

    @Test
    void impedeSegundaInstanciaViaReflection() throws Exception {
        GeradorProtocolo.getInstancia();
        Constructor<GeradorProtocolo> construtor = GeradorProtocolo.class.getDeclaredConstructor();
        construtor.setAccessible(true);

        assertThatThrownBy(construtor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(IllegalStateException.class);
    }
}
