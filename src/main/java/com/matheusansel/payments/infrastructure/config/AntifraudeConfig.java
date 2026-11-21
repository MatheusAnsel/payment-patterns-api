package com.matheusansel.payments.infrastructure.config;

import com.matheusansel.payments.domain.validation.HorarioValidador;
import com.matheusansel.payments.domain.validation.PedidoDuplicadoValidador;
import com.matheusansel.payments.domain.validation.ValidadorAntifraude;
import com.matheusansel.payments.domain.validation.ValorMaximoValidador;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Monta a cadeia antifraude em um unico lugar. A ordem importa: as regras baratas
 * (em memoria) vem antes da que consulta o banco.
 */
@Configuration
public class AntifraudeConfig {

    @Bean
    public ValidadorAntifraude cadeiaAntifraude(PagamentoRepository repository, Clock clock) {
        ValidadorAntifraude inicio = new ValorMaximoValidador();
        inicio.seguir(new HorarioValidador(clock))
                .seguir(new PedidoDuplicadoValidador(repository));
        return inicio;
    }
}
