package com.matheusansel.payments.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relogio injetavel: as regras que dependem de data/hora (vencimento do boleto,
 * antifraude por horario) ficam testaveis com Clock.fixed.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(FUSO_BRASIL);
    }
}
