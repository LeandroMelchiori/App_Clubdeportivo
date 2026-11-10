package com.example.clubdeportivo.backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Club deportivo API")
            .version("v1")
            .description("API del servicio multi-club de Club deportivo."),
    )
}
