package com.university.sigac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** REST API specification (Swagger UI at /swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sigacOpenApi() {
        return new OpenAPI()
                .info(new Info().title("SIGAC API").version("1.0.0")
                        .description("Information System for Minutes and Council Management"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
