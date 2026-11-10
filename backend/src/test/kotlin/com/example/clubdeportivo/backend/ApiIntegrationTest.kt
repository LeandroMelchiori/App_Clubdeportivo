package com.example.clubdeportivo.backend

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
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
}
