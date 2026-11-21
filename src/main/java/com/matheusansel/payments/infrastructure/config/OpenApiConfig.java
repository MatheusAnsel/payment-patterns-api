package com.matheusansel.payments.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Patterns API")
                .version("1.0.0")
                .description("API de pagamentos de pedidos (Pix, cartao de credito e boleto) "
                        + "construida para demonstrar Strategy, Factory, Facade, Observer, Singleton "
                        + "e Chain of Responsibility."));
    }
}
