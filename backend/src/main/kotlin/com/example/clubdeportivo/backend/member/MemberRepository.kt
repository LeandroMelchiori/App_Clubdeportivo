package com.example.clubdeportivo.backend.member

import com.example.clubdeportivo.backend.club.Role
import com.example.clubdeportivo.backend.db.DatabaseContext
import java.sql.ResultSet
import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

data class Member(val userId: UUID, val name: String, val email: String, val role: Role, val active: Boolean)

/**
 * Miembros del club activo. Contexto de club: todas las consultas filtran por [DatabaseContext.currentClubId]
 * y, si alguna se olvidara, las reglas por fila de la base harían lo mismo.
 */
@Repository
class MemberRepository(private val jdbc: JdbcClient, private val db: DatabaseContext) {

    fun list(): List<Member> = jdbc.sql("$SELECT WHERE m.club_id = :club ORDER BY u.name, u.id")
        .param("club", db.currentClubId())
        .query(::map)
        .list()

    fun find(userId: UUID): Member? = jdbc.sql("$SELECT WHERE m.club_id = :club AND m.user_id = :user")
        .param("club", db.currentClubId())
        .param("user", userId)
        .query(::map)
        .optional()
        .orElse(null)

    /** Busca la cuenta en toda la base, aunque sea de otro club. Solo devuelve el id. */
    fun userIdByEmail(email: String): UUID? = jdbc.sql("SELECT user_id_by_email(:email)")
        .param("email", email)
        .query(UUID::class.java)
        .optional()
        .orElse(null)

    fun insert(userId: UUID, role: Role) {
        jdbc.sql("INSERT INTO club_memberships (user_id, club_id, role) VALUES (:user, :club, :role)")
            .param("user", userId)
            .param("club", db.currentClubId())
            .param("role", role.name)
            .update()
    }

    fun reactivate(userId: UUID, role: Role) {
        jdbc.sql(
            """
            UPDATE club_memberships SET active = true, removed_at = NULL, role = :role
            WHERE club_id = :club AND user_id = :user
            """,
        )
            .param("role", role.name)
            .param("club", db.currentClubId())
            .param("user", userId)
            .update()
    }

    /** Devuelve false si la persona no era miembro activo del club. */
    fun deactivate(userId: UUID): Boolean = jdbc.sql(
        """
        UPDATE club_memberships SET active = false, removed_at = now()
        WHERE club_id = :club AND user_id = :user AND active
        """,
    )
        .param("club", db.currentClubId())
        .param("user", userId)
        .update() == 1

    /** Cierra las sesiones de la persona en el club activo. Las que tenga en otros clubes siguen. */
    fun revokeSessions(userId: UUID) {
        jdbc.sql("UPDATE sessions SET revoked_at = now() WHERE club_id = :club AND user_id = :user AND revoked_at IS NULL")
            .param("club", db.currentClubId())
            .param("user", userId)
            .update()
    }

    private fun map(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) = Member(
        userId = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        email = rs.getString("email"),
        role = Role.valueOf(rs.getString("role")),
        active = rs.getBoolean("active"),
    )

    private companion object {
        const val SELECT =
            "SELECT u.id, u.name, u.email, m.role, m.active FROM club_memberships m JOIN users u ON u.id = m.user_id"
    }
}
