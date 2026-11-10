package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.club.Role
import java.time.Duration
import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/** Un club al que la cuenta tiene acceso, con su rol en él. */
data class ClubAccess(val clubId: UUID, val clubName: String, val role: Role)

data class StoredRefreshToken(
    val sessionId: UUID,
    val userId: UUID,
    val clubId: UUID,
    val sessionRevoked: Boolean,
    val used: Boolean,
    val expired: Boolean,
)

/**
 * Membresías, sesiones y tokens de renovación vistos desde la identidad, es decir, de todos los clubes
 * de una cuenta. Se usa solo en el contexto de identidad.
 */
@Repository
class SessionRepository(private val jdbc: JdbcClient) {

    /** Primero el club del último ingreso; si nunca ingresó, el más antiguo. */
    fun activeClubsOf(userId: UUID): List<ClubAccess> = jdbc.sql(
        """
        SELECT c.id, c.name, m.role
        FROM club_memberships m JOIN clubs c ON c.id = m.club_id
        WHERE m.user_id = :user AND m.active
        ORDER BY m.last_login_at DESC NULLS LAST, m.created_at, c.id
        """,
    )
        .param("user", userId)
        .query { rs, _ -> ClubAccess(rs.getObject("id", UUID::class.java), rs.getString("name"), Role.valueOf(rs.getString("role"))) }
        .list()

    fun insertMembership(userId: UUID, clubId: UUID, role: Role) {
        jdbc.sql("INSERT INTO club_memberships (user_id, club_id, role) VALUES (:user, :club, :role)")
            .param("user", userId)
            .param("club", clubId)
            .param("role", role.name)
            .update()
    }

    fun recordLogin(userId: UUID, clubId: UUID) {
        jdbc.sql("UPDATE club_memberships SET last_login_at = now() WHERE user_id = :user AND club_id = :club")
            .param("user", userId)
            .param("club", clubId)
            .update()
    }

    fun createSession(sessionId: UUID, userId: UUID, clubId: UUID) {
        jdbc.sql("INSERT INTO sessions (id, user_id, club_id) VALUES (:id, :user, :club)")
            .param("id", sessionId)
            .param("user", userId)
            .param("club", clubId)
            .update()
    }

    fun switchClub(sessionId: UUID, clubId: UUID) {
        jdbc.sql("UPDATE sessions SET club_id = :club WHERE id = :id").param("club", clubId).param("id", sessionId).update()
    }

    fun revokeSession(sessionId: UUID) {
        jdbc.sql("UPDATE sessions SET revoked_at = now() WHERE id = :id AND revoked_at IS NULL")
            .param("id", sessionId)
            .update()
    }

    fun revokeAllSessionsOf(userId: UUID) {
        jdbc.sql("UPDATE sessions SET revoked_at = now() WHERE user_id = :user AND revoked_at IS NULL")
            .param("user", userId)
            .update()
    }

    fun insertRefreshToken(tokenHash: ByteArray, sessionId: UUID, ttl: Duration) {
        jdbc.sql(
            """
            INSERT INTO refresh_tokens (token_hash, session_id, expires_at)
            VALUES (:hash, :session, now() + make_interval(secs => :ttl))
            """,
        )
            .param("hash", tokenHash)
            .param("session", sessionId)
            .param("ttl", ttl.toSeconds().toDouble())
            .update()
    }

    /** Bloquea el token y su sesión hasta el final de la transacción, así dos renovaciones no se pisan. */
    fun findRefreshTokenForUpdate(tokenHash: ByteArray): StoredRefreshToken? = jdbc.sql(
        """
        SELECT rt.session_id, s.user_id, s.club_id,
               s.revoked_at IS NOT NULL AS session_revoked,
               rt.used_at IS NOT NULL AS used,
               rt.expires_at <= now() AS expired
        FROM refresh_tokens rt JOIN sessions s ON s.id = rt.session_id
        WHERE rt.token_hash = :hash
        FOR UPDATE OF rt, s
        """,
    )
        .param("hash", tokenHash)
        .query { rs, _ ->
            StoredRefreshToken(
                sessionId = rs.getObject("session_id", UUID::class.java),
                userId = rs.getObject("user_id", UUID::class.java),
                clubId = rs.getObject("club_id", UUID::class.java),
                sessionRevoked = rs.getBoolean("session_revoked"),
                used = rs.getBoolean("used"),
                expired = rs.getBoolean("expired"),
            )
        }
        .optional()
        .orElse(null)

    fun markRefreshTokenUsed(tokenHash: ByteArray) {
        jdbc.sql("UPDATE refresh_tokens SET used_at = now() WHERE token_hash = :hash").param("hash", tokenHash).update()
    }
}
