package com.example.clubdeportivo.backend.member

import com.example.clubdeportivo.backend.IntegrationTest
import com.example.clubdeportivo.backend.auth.AuthResponse
import com.example.clubdeportivo.backend.club.Role
import java.util.function.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

/** Alta y baja de personal, contraseña temporal y permisos por rol en la administración del club. */
class StaffIntegrationTest : IntegrationTest() {

    @Test
    fun personalNuevo_ingresaConContraseñaTemporal_yDebeCambiarlaAntesDeOperar() {
        val club = createClub()
        val admin = login(club.adminEmail, PASSWORD)
        val email = uniqueEmail("recepcion")

        val alta = addMember(admin.accessToken, email, Role.RECEPTION, name = "Ana Recepción")

        assertThat(alta.existingAccount).isFalse()
        assertThat(alta.member.role).isEqualTo(Role.RECEPTION)
        val temporal = checkNotNull(alta.temporaryPassword)
        assertThat(temporal).hasSize(12)

        val primerIngreso = login(email, temporal)
        assertThat(primerIngreso.passwordChangeRequired).isTrue()
        get("/api/v1/club", primerIngreso.accessToken)
            .expectStatus().isForbidden
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Antes de continuar hay que cambiar la contraseña temporal.")

        changePassword(primerIngreso.accessToken, "no-es-la-temporal", "nueva-clave-segura")
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.title").isEqualTo("Contraseña actual incorrecta")
        changePassword(primerIngreso.accessToken, temporal, temporal)
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.title").isEqualTo("Contraseña repetida")
        val cambiada = changePassword(primerIngreso.accessToken, temporal, "nueva-clave-segura")
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java).returnResult().responseBody!!

        assertThat(cambiada.passwordChangeRequired).isFalse()
        get("/api/v1/club", cambiada.accessToken).expectStatus().isOk
        // El cambio cierra las sesiones anteriores y la temporal deja de servir.
        refreshRequest(primerIngreso.refreshToken).expectStatus().isUnauthorized
        loginRequest(email, temporal).expectStatus().isUnauthorized
        assertThat(login(email, "nueva-clave-segura").passwordChangeRequired).isFalse()
    }

    @Test
    fun soloAdministracion_gestionaElPersonal() {
        val club = createClub()
        val admin = login(club.adminEmail, PASSWORD)
        val email = uniqueEmail("caja")
        val temporal = checkNotNull(addMember(admin.accessToken, email, Role.CASHIER).temporaryPassword)
        val caja = changePassword(login(email, temporal).accessToken, temporal, "clave-de-caja-123")
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java).returnResult().responseBody!!

        get("/api/v1/club", caja.accessToken).expectStatus().isOk
        get("/api/v1/members", caja.accessToken)
            .expectStatus().isForbidden
            .expectBody().jsonPath("$.detail").isEqualTo("El rol en este club no permite esta operación.")
        client.post().uri("/api/v1/members")
            .headers { it.setBearerAuth(caja.accessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("name" to "Otra", "email" to uniqueEmail("otra"), "role" to "ADMIN"))
            .exchange()
            .expectStatus().isForbidden
        get("/api/v1/members", admin.accessToken)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[*].role").value(Consumer<List<String>> { assertThat(it).containsExactlyInAnyOrder("ADMIN", "CASHIER") })
    }

    @Test
    fun alta_validaLosDatos_yRechazaDuplicados() {
        val club = createClub()
        val admin = login(club.adminEmail, PASSWORD)

        client.post().uri("/api/v1/members")
            .headers { it.setBearerAuth(admin.accessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("name" to "Sin datos", "email" to "no-es-un-email"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.errors[*].field").value(Consumer<List<String>> { assertThat(it).containsExactlyInAnyOrder("email", "role") })

        val email = uniqueEmail("repetida")
        addMember(admin.accessToken, email, Role.READ_ONLY)
        addMemberRequest(admin.accessToken, email.uppercase(), Role.CASHIER)
            .expectStatus().isEqualTo(409)
            .expectBody().jsonPath("$.detail").isEqualTo("Esa persona ya es miembro del club.")
    }

    @Test
    fun baja_cierraLasSesionesEnElClub_yLaReincorporacionConservaLaCuenta() {
        val club = createClub()
        val admin = login(club.adminEmail, PASSWORD)
        val email = uniqueEmail("baja")
        val alta = addMember(admin.accessToken, email, Role.RECEPTION)
        val temporal = checkNotNull(alta.temporaryPassword)
        val sesion = changePassword(login(email, temporal).accessToken, temporal, "clave-de-recepcion")
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java).returnResult().responseBody!!

        delete("/api/v1/members/${alta.member.userId}", admin.accessToken).expectStatus().isNoContent

        refreshRequest(sesion.refreshToken).expectStatus().isUnauthorized
        loginRequest(email, "clave-de-recepcion")
            .expectStatus().isForbidden
            .expectBody().jsonPath("$.title").isEqualTo("Sin clubes activos")
        delete("/api/v1/members/${alta.member.userId}", admin.accessToken).expectStatus().isNotFound

        val reincorporada = addMember(admin.accessToken, email, Role.CASHIER)
        assertThat(reincorporada.existingAccount).isTrue()
        assertThat(reincorporada.temporaryPassword).isNull()
        assertThat(login(email, "clave-de-recepcion").club.role).isEqualTo(Role.CASHIER)
    }

    @Test
    fun administrador_noPuedeDarseDeBajaASiMismo() {
        val club = createClub()
        val admin = login(club.adminEmail, PASSWORD)

        delete("/api/v1/members/${club.adminUserId}", admin.accessToken)
            .expectStatus().isEqualTo(409)
            .expectBody().jsonPath("$.detail").isEqualTo("No es posible darse de baja a uno mismo.")
    }

    @Test
    fun altaDeClub_conEmailExistente_respondeConflicto() {
        val club = createClub()

        client.post().uri("/api/v1/clubs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("clubName" to "Otro", "adminName" to "Otra", "adminEmail" to club.adminEmail, "adminPassword" to PASSWORD))
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody().jsonPath("$.detail").isEqualTo("Ya existe una cuenta con ese email.")
    }

    private fun addMemberRequest(accessToken: String, email: String, role: Role): RestTestClient.ResponseSpec =
        client.post().uri("/api/v1/members")
            .headers { it.setBearerAuth(accessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("name" to "Persona", "email" to email, "role" to role))
            .exchange()

    private fun changePassword(accessToken: String, current: String, new: String): RestTestClient.ResponseSpec =
        client.post().uri("/api/v1/auth/password")
            .headers { it.setBearerAuth(accessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("currentPassword" to current, "newPassword" to new))
            .exchange()

    private fun delete(path: String, accessToken: String): RestTestClient.ResponseSpec =
        client.delete().uri(path).headers { it.setBearerAuth(accessToken) }.exchange()
}
