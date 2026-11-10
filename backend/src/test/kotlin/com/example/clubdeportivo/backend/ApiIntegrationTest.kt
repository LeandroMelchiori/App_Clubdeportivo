package com.example.clubdeportivo.backend

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

/** El esqueleto de la API: migraciones, salud, documentación y formato de los errores. */
class ApiIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var flyway: Flyway

    @Test
    fun flyway_aplicaTodasLasMigraciones() {
        val info = flyway.info()

        assertThat(info.current()?.version?.version).isEqualTo("3")
        assertThat(info.pending()).isEmpty()
        val comentario = jdbc.sql("select obj_description('public'::regnamespace, 'pg_namespace')")
            .query(String::class.java)
            .single()
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
            .jsonPath("$.paths['/api/v1/auth/login'].post").exists()
            .jsonPath("$.components.securitySchemes.bearer.scheme").isEqualTo("bearer")
    }

    @Test
    fun sinToken_respondeProblemDetail401() {
        client.get().uri("/api/v1/club")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.title").isEqualTo("No autenticado")
            .jsonPath("$.instance").isEqualTo("/api/v1/club")
    }

    @Test
    fun rutaInexistente_respondeProblemDetail() {
        val token = login(createClub().adminEmail, PASSWORD).accessToken

        get("/api/v1/no-existe", token)
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
