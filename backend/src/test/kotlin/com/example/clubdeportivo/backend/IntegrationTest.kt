package com.example.clubdeportivo.backend

import com.example.clubdeportivo.backend.auth.AuthResponse
import com.example.clubdeportivo.backend.club.CreateClubResponse
import com.example.clubdeportivo.backend.club.Role
import com.example.clubdeportivo.backend.member.AddMemberResponse
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.servlet.client.RestTestClient

/**
 * Base de los tests de integración: la API completa contra un PostgreSQL real. Todas las clases usan la
 * misma configuración, así comparten el contexto de Spring y el contenedor.
 *
 * Cada test crea sus propios clubes y cuentas, con emails únicos, y no depende de los demás.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["club.auth.jwt-secret=${IntegrationTest.TEST_JWT_SECRET}"],
)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTest {

    @Autowired
    protected lateinit var client: RestTestClient

    @Autowired
    protected lateinit var jdbc: JdbcClient

    protected fun createClub(name: String = "Club ${UUID.randomUUID().toString().take(8)}"): ClubFixture {
        val email = uniqueEmail("admin")
        val response = client.post().uri("/api/v1/clubs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("clubName" to name, "adminName" to "Admin de $name", "adminEmail" to email, "adminPassword" to PASSWORD))
            .exchange()
            .expectStatus().isCreated
            .expectBody(CreateClubResponse::class.java)
            .returnResult()
            .responseBody!!
        return ClubFixture(response.clubId, name, response.adminUserId, email, PASSWORD)
    }

    protected fun login(email: String, password: String, clubId: UUID? = null): AuthResponse =
        loginRequest(email, password, clubId)
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java)
            .returnResult()
            .responseBody!!

    protected fun loginRequest(email: String, password: String, clubId: UUID? = null): RestTestClient.ResponseSpec =
        client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("email" to email, "password" to password, "clubId" to clubId))
            .exchange()

    protected fun refreshRequest(refreshToken: String, clubId: UUID? = null): RestTestClient.ResponseSpec =
        client.post().uri("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("refreshToken" to refreshToken, "clubId" to clubId))
            .exchange()

    protected fun refresh(refreshToken: String, clubId: UUID? = null): AuthResponse =
        refreshRequest(refreshToken, clubId)
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java)
            .returnResult()
            .responseBody!!

    protected fun addMember(accessToken: String, email: String, role: Role, name: String = "Persona del club"): AddMemberResponse =
        client.post().uri("/api/v1/members")
            .headers { it.setBearerAuth(accessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("name" to name, "email" to email, "role" to role))
            .exchange()
            .expectStatus().isCreated
            .expectBody(AddMemberResponse::class.java)
            .returnResult()
            .responseBody!!

    protected fun get(path: String, accessToken: String): RestTestClient.ResponseSpec =
        client.get().uri(path).headers { it.setBearerAuth(accessToken) }.exchange()

    protected fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"

    data class ClubFixture(val clubId: UUID, val name: String, val adminUserId: UUID, val adminEmail: String, val adminPassword: String)

    companion object {
        /** Clave solo para tests. La real sale de CLUB_AUTH_JWT_SECRET. */
        const val TEST_JWT_SECRET = "Y2xhdmUtZGUtZmlybWEtc29sby1wYXJhLXRlc3RzLW5vLXVzYXItZW4tcHJvZHVjY2lvbg=="
        const val PASSWORD = "clave-segura-123"
    }
}
