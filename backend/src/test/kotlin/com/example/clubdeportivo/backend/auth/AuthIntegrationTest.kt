package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.IntegrationTest
import com.example.clubdeportivo.backend.club.Role
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import java.util.function.Consumer
import javax.crypto.spec.SecretKeySpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

class AuthIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var jwtEncoder: JwtEncoder

    @Autowired
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var properties: AuthProperties

    @Test
    fun login_conUnSoloClub_abreSesionEnEseClub() {
        val club = createClub()

        val auth = login(club.adminEmail.uppercase(), PASSWORD)

        assertThat(auth.tokenType).isEqualTo("Bearer")
        assertThat(auth.expiresIn).isEqualTo(900)
        assertThat(auth.club).isEqualTo(ClubAccessResponse(club.clubId, club.name, Role.ADMIN))
        assertThat(auth.clubs).containsExactly(auth.club)
        assertThat(auth.passwordChangeRequired).isFalse()
        val jwt = jwtDecoder.decode(auth.accessToken)
        assertThat(jwt.subject).isEqualTo(club.adminUserId.toString())
        assertThat(jwt.getClaimAsString("club")).isEqualTo(club.clubId.toString())
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN")
        assertThat(Duration.between(jwt.issuedAt, jwt.expiresAt)).isEqualTo(Duration.ofMinutes(15))
        assertThat(jwt.claims.keys).doesNotContain("email", "name")
        get("/api/v1/club", auth.accessToken)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(club.clubId.toString())
            .jsonPath("$.role").isEqualTo("ADMIN")
    }

    @Test
    fun login_conPasswordIncorrectaOEmailInexistente_respondeLoMismo() {
        val club = createClub()

        val conPasswordIncorrecta = loginRequest(club.adminEmail, "otra-clave-123")
            .expectStatus().isUnauthorized
            .expectBody(String::class.java).returnResult().responseBody
        val conEmailInexistente = loginRequest(uniqueEmail("nadie"), "otra-clave-123")
            .expectStatus().isUnauthorized
            .expectBody(String::class.java).returnResult().responseBody

        assertThat(conPasswordIncorrecta).contains("Credenciales inválidas")
        assertThat(conPasswordIncorrecta).isEqualTo(conEmailInexistente)
    }

    @Test
    fun login_conCamposVacios_respondeErroresPorCampo() {
        client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("email" to " "))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.title").isEqualTo("Datos inválidos")
            .jsonPath("$.errors[?(@.field == 'email')].message").value(Consumer<List<String>> { assertThat(it).containsExactly("El email es obligatorio.") })
            .jsonPath("$.errors[?(@.field == 'password')].message").value(Consumer<List<String>> { assertThat(it).containsExactly("La contraseña es obligatoria.") })
    }

    @Test
    fun refresh_rotaElToken_yReusarElAnteriorRevocaLaSesion() {
        val club = createClub()
        val primero = login(club.adminEmail, PASSWORD)

        val segundo = refresh(primero.refreshToken)

        assertThat(segundo.refreshToken).isNotEqualTo(primero.refreshToken)
        assertThat(jwtDecoder.decode(segundo.accessToken).getClaimAsString("sid"))
            .isEqualTo(jwtDecoder.decode(primero.accessToken).getClaimAsString("sid"))
        refreshRequest(primero.refreshToken)
            .expectStatus().isUnauthorized
            .expectBody().jsonPath("$.title").isEqualTo("Sesión inválida")
        // El token reusado delata una copia: la sesión entera se revoca, también el token nuevo.
        refreshRequest(segundo.refreshToken).expectStatus().isUnauthorized
    }

    @Test
    fun logout_revocaLaSesion_yNoDelataTokensDesconocidos() {
        val auth = login(createClub().adminEmail, PASSWORD)

        logout(auth.refreshToken)
        logout("token-que-no-existe")

        refreshRequest(auth.refreshToken).expectStatus().isUnauthorized
    }

    @Test
    fun refresh_conTokenVencido_respondeSesionInvalida() {
        val auth = login(createClub().adminEmail, PASSWORD)
        jdbc.sql("UPDATE refresh_tokens SET expires_at = now() - interval '1 second' WHERE token_hash = :hash")
            .param("hash", RefreshTokens.hash(auth.refreshToken))
            .update()

        refreshRequest(auth.refreshToken).expectStatus().isUnauthorized
    }

    @Test
    fun tokenDeAccesoVencido_falsificadoOMalformado_respondeNoAutenticado() {
        val auth = login(createClub().adminEmail, PASSWORD)
        val claims = jwtDecoder.decode(auth.accessToken)
        val principal = ClubAccess(UUID.fromString(claims.getClaimAsString("club")), "", Role.ADMIN)
        val sessionId = UUID.fromString(claims.getClaimAsString("sid"))
        val userId = UUID.fromString(claims.subject)
        val haceUnaHora = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC)
        val vencido = AccessTokenIssuer(jwtEncoder, properties, haceUnaHora).issue(userId, principal, sessionId, false)
        val otraClave = SecretKeySpec(Base64.getDecoder().decode(TEST_JWT_SECRET).reversedArray(), "HmacSHA256")
        val falsificado = AccessTokenIssuer(NimbusJwtEncoder(ImmutableSecret<SecurityContext>(otraClave)), properties, Clock.systemUTC())
            .issue(userId, principal, sessionId, false)

        for (token in listOf(vencido, falsificado, "no-es-un-jwt")) {
            get("/api/v1/club", token)
                .expectStatus().isUnauthorized
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("No autenticado")
        }
    }

    @Test
    fun cuentaConVariosClubes_eligeClubAlIngresarYCambiaDeClubConRefresh() {
        val clubA = createClub()
        val clubB = createClub()
        val adminB = login(clubB.adminEmail, PASSWORD)
        val alta = addMember(adminB.accessToken, clubA.adminEmail, Role.RECEPTION)
        assertThat(alta.existingAccount).isTrue()
        assertThat(alta.temporaryPassword).isNull()

        // Sin clubId: el del último ingreso, o el más antiguo si nunca ingresó a otro.
        val enA = login(clubA.adminEmail, PASSWORD)
        assertThat(enA.club.id).isEqualTo(clubA.clubId)
        assertThat(enA.clubs.map { it.id to it.role })
            .containsExactlyInAnyOrder(clubA.clubId to Role.ADMIN, clubB.clubId to Role.RECEPTION)

        val enB = login(clubA.adminEmail, PASSWORD, clubB.clubId)
        assertThat(enB.club).isEqualTo(ClubAccessResponse(clubB.clubId, clubB.name, Role.RECEPTION))
        assertThat(login(clubA.adminEmail, PASSWORD).club.id).isEqualTo(clubB.clubId)

        val deVueltaEnA = refresh(enB.refreshToken, clubA.clubId)
        assertThat(deVueltaEnA.club.role).isEqualTo(Role.ADMIN)
        get("/api/v1/club", deVueltaEnA.accessToken)
            .expectStatus().isOk
            .expectBody().jsonPath("$.id").isEqualTo(clubA.clubId.toString())
    }

    @Test
    fun limitePorCuenta_bloqueaTrasCincoFallos_sinAfectarOtrasCuentas() {
        val club = createClub()
        val otro = createClub()

        repeat(5) { loginRequest(club.adminEmail, "clave-incorrecta").expectStatus().isUnauthorized }

        loginRequest(club.adminEmail, PASSWORD)
            .expectStatus().isEqualTo(429)
            .expectHeader().exists(HttpHeaders.RETRY_AFTER)
            .expectBody().jsonPath("$.title").isEqualTo("Demasiados intentos")
        login(otro.adminEmail, PASSWORD)
    }

    @Test
    fun limitePorIp_bloqueaTrasVeinteFallosConCuentasDistintas() {
        val club = createClub()

        repeat(20) { loginRequest(uniqueEmail("intento"), "clave-incorrecta").expectStatus().isUnauthorized }

        loginRequest(club.adminEmail, PASSWORD).expectStatus().isEqualTo(429)
    }

    private fun logout(refreshToken: String) {
        client.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("refreshToken" to refreshToken))
            .exchange()
            .expectStatus().isNoContent
    }
}
