package com.nexora.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI nexoraOpenApi() {
        return new OpenAPI().info(new Info()
                .title("NEXORA Integrated Management Platform API")
                .version("1.0.0")
                .description("Қойма, емхана, әуе рейстері (тек оқу демонстрациясы), құжат айналымы және "
                        + "хабарландырулар. Аутентификация: сеанс cookie + X-XSRF-TOKEN тақырыбы. "
                        + "Алдымен POST /api/auth/login арқылы жүйеге кіріңіз."));
    }
}
