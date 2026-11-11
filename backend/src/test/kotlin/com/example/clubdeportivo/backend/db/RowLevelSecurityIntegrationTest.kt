package com.example.clubdeportivo.backend.db

import com.example.clubdeportivo.backend.IntegrationTest
import com.example.clubdeportivo.backend.club.Role
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException

/**
 * Las reglas por fila de PostgreSQL como respaldo: estas consultas se olvidan a propósito del filtro por
 * club y la base igual no deja ver ni tocar datos de otro club.
 */
class RowLevelSecurityIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var db: DatabaseContext

    private lateinit var clubA: ClubFixture
    private lateinit var clubB: ClubFixture

    @BeforeEach
    fun dosClubesConSesiones() {
        clubA = createClub()
        clubB = createClub()
        login(clubA.adminEmail, PASSWORD)
        val adminB = login(clubB.adminEmail, PASSWORD).accessToken
        addMember(adminB, uniqueEmail("de-b"), Role.CASHIER)
    }

    @Test
    fun consultasSinFiltro_soloVenElClubActivo() {
        db.inClub(clubA.clubId) {
            assertThat(uuids("SELECT id FROM clubs")).containsExactly(clubA.clubId)
            assertThat(uuids("SELECT club_id FROM club_memberships")).containsOnly(clubA.clubId)
            assertThat(uuids("SELECT club_id FROM sessions")).containsOnly(clubA.clubId)
            assertThat(uuids("SELECT id FROM users")).containsExactly(clubA.adminUserId)
        }
    }

    @Test
    fun escriturasHaciaOtroClub_fallanOnoAfectanFilas() {
        assertThatThrownBy {
            db.inClub(clubA.clubId) {
                jdbc.sql("INSERT INTO club_memberships (user_id, club_id, role) VALUES (:user, :club, 'ADMIN')")
                    .param("user", clubA.adminUserId)
                    .param("club", clubB.clubId)
                    .update()
            }
        }.isInstanceOf(DataAccessException::class.java).rootCause().hasMessageContaining("row-level security")

        val actualizadas = db.inClub(clubA.clubId) {
            jdbc.sql("UPDATE sessions SET revoked_at = now() WHERE club_id = :club").param("club", clubB.clubId).update()
        }
        assertThat(actualizadas).isZero()
        val sesionesDeBRevocadas = jdbc.sql("SELECT count(*) FROM sessions WHERE club_id = :club AND revoked_at IS NOT NULL")
            .param("club", clubB.clubId)
            .query(Long::class.java)
            .single()
        assertThat(sesionesDeBRevocadas).isZero()
    }

    @Test
    fun dentroDeUnClub_noHayAccesoAHashesNiTokens() {
        for (consulta in listOf("SELECT password_hash FROM users", "SELECT token_hash FROM refresh_tokens")) {
            assertThatThrownBy { db.inClub(clubA.clubId) { jdbc.sql(consulta).query().listOfRows() } }
                .isInstanceOf(DataAccessException::class.java)
                .rootCause()
                .hasMessageContaining("permission denied")
        }
    }

    @Test
    fun conElRolDeClubPeroSinClubActivo_noSeVeNada() {
        val visibles = db.identity {
            jdbc.sql("SET LOCAL ROLE club_app").update()
            jdbc.sql("SELECT count(*) FROM club_memberships").query(Long::class.java).single()
        }

        assertThat(visibles).isZero()
    }

    @Test
    fun repositoriosDeClub_fallanFueraDeContexto_yLosContextosNoSeAnidan() {
        assertThatThrownBy { db.currentClubId() }.isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { db.identity { db.inClub(clubA.clubId) {} } }.isInstanceOf(IllegalStateException::class.java)
    }

    private fun uuids(sql: String): List<UUID> = jdbc.sql(sql).query(UUID::class.java).list().filterNotNull()
}
