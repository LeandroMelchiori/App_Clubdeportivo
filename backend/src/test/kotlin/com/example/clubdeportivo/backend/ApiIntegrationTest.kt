package com.example.clubdeportivo.backend

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.client.RestTestClient

/** Levanta la API completa contra un PostgreSQL real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration::class)
class ApiIntegrationTest @Autowired constructor(
    private val client: RestTestClient,
    private val flyway: Flyway,
    private val jdbc: JdbcTemplate,
) {

    @Test
    fun flyway_aplicaLaMigracionInicial() {
        val info = flyway.info()

        assertThat(info.current()?.version?.version).isEqualTo("1")
        assertThat(info.pending()).isEmpty()
        val comentario = jdbc.queryForObject(
            "select obj_description('public'::regnamespace, 'pg_namespace')",
            String::class.java,
        )
        assertThat(comentario).isEqualTo("Club deportivo: datos del servicio multi-club")
    }

    @Test
    fun health_respondeUpConLaBaseDisponible() {
        client.get().uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
    }

    @Test
    fun openApi_documentaLaApiBajoApiV1() {
        client.get().uri("/api/v1/openapi")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.info.title").isEqualTo("Club deportivo API")
            .jsonPath("$.info.version").isEqualTo("v1")
            .jsonPath("$.paths['/api/v1/health'].get").exists()
    }

    @Test
    fun rutaInexistente_respondeProblemDetail() {
        client.get().uri("/api/v1/no-existe")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.title").isEqualTo("Recurso no encontrado")
            .jsonPath("$.detail").isEqualTo("No existe el recurso solicitado.")
            .jsonPath("$.instance").isEqualTo("/api/v1/no-existe")
    }

    @Test
    fun metodoNoPermitido_respondeProblemDetail() {
        client.delete().uri("/api/v1/health")
            .exchange()
            .expectStatus().isEqualTo(405)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(405)
            .jsonPath("$.title").isEqualTo("Método no permitido")
            .jsonPath("$.detail").isEqualTo("El método DELETE no está permitido para este recurso.")
    }
}
