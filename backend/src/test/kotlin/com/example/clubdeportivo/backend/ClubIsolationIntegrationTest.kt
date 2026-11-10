package com.example.clubdeportivo.backend

import com.example.clubdeportivo.backend.club.Role
import com.example.clubdeportivo.backend.member.MemberResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * Cada endpoint que toca datos de un club intenta leer o escribir los de otro, y tiene que fallar.
 * El club sale siempre del token: ninguna ruta lo recibe como parámetro.
 */
class ClubIsolationIntegrationTest : IntegrationTest() {

    private lateinit var clubA: ClubFixture
    private lateinit var clubB: ClubFixture
    private lateinit var adminA: String
    private lateinit var adminB: String
    private lateinit var miembroDeB: MemberResponse

    @BeforeEach
    fun dosClubes() {
        clubA = createClub()
        clubB = createClub()
        adminA = login(clubA.adminEmail, PASSWORD).accessToken
        adminB = login(clubB.adminEmail, PASSWORD).accessToken
        miembroDeB = addMember(adminB, uniqueEmail("de-b"), Role.RECEPTION).member
    }

    @Test
    fun club_devuelveSoloElClubDelToken() {
        get("/api/v1/club", adminA).expectStatus().isOk.expectBody().jsonPath("$.id").isEqualTo(clubA.clubId.toString())
        get("/api/v1/club", adminB).expectStatus().isOk.expectBody().jsonPath("$.id").isEqualTo(clubB.clubId.toString())
    }

    @Test
    fun listarMiembros_noMuestraLosDeOtroClub() {
        val deA = members(adminA)

        assertThat(deA.map { it.userId }).containsExactly(clubA.adminUserId)
        assertThat(members(adminB).map { it.userId }).containsExactlyInAnyOrder(clubB.adminUserId, miembroDeB.userId)
    }

    @Test
    fun sumarMiembro_noAceptaElClubComoParametro() {
        client.post().uri("/api/v1/members")
            .headers { it.setBearerAuth(adminA) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("name" to "Intrusa", "email" to uniqueEmail("intrusa"), "role" to "ADMIN", "clubId" to clubB.clubId))
            .exchange()
            .expectStatus().isBadRequest

        assertThat(members(adminB).map { it.userId }).containsExactlyInAnyOrder(clubB.adminUserId, miembroDeB.userId)
        assertThat(members(adminA).map { it.userId }).containsExactly(clubA.adminUserId)
    }

    @Test
    fun darDeBaja_aUnMiembroDeOtroClub_respondeNoEncontrado() {
        client.delete().uri("/api/v1/members/${miembroDeB.userId}")
            .headers { it.setBearerAuth(adminA) }
            .exchange()
            .expectStatus().isNotFound

        assertThat(members(adminB).single { it.userId == miembroDeB.userId }.active).isTrue()
    }

    @Test
    fun ingresar_oRenovar_haciaUnClubAjeno_respondeSinAcceso() {
        loginRequest(clubA.adminEmail, PASSWORD, clubB.clubId)
            .expectStatus().isForbidden
            .expectBody().jsonPath("$.title").isEqualTo("Sin acceso al club")

        val sesion = login(clubA.adminEmail, PASSWORD)
        refreshRequest(sesion.refreshToken, clubB.clubId).expectStatus().isForbidden
        // El pedido rechazado no consume el token: la sesión sigue en su club.
        assertThat(refresh(sesion.refreshToken).club.id).isEqualTo(clubA.clubId)
    }

    private fun members(accessToken: String): List<MemberResponse> =
        get("/api/v1/members", accessToken)
            .expectStatus().isOk
            .expectBody(Array<MemberResponse>::class.java)
            .returnResult()
            .responseBody!!
            .toList()
}
