package com.example.clubdeportivo.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        // URL relativa: detrás del proxy la API recibe HTTP, y una URL armada con ese esquema haría que
        // Swagger UI, servida por HTTPS, llame a la API por HTTP.
        .servers(listOf(Server().url("/").description("Esta misma API")))
        .info(
            Info()
                .title("Club deportivo API")
                .version("v1")
                .description("API del servicio multi-club de Club deportivo."),
        )
        .components(
            Components().addSecuritySchemes(
                "bearer",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token de acceso de /api/v1/auth/login o /api/v1/auth/refresh."),
            ),
        )
}
