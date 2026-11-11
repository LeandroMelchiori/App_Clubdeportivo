package com.example.clubdeportivo.backend.user

import java.sql.ResultSet
import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

data class UserAccount(
    val id: UUID,
    val email: String,
    val name: String,
    val passwordHash: String,
    val passwordChangeRequired: Boolean,
)

/**
 * Cuentas de usuario. Las lecturas traen el hash de la contraseña, así que solo funcionan en el contexto
 * de identidad: `club_app` no tiene permiso sobre esa columna.
 */
@Repository
class UserRepository(private val jdbc: JdbcClient) {

    fun findByEmail(email: String): UserAccount? =
        jdbc.sql("$SELECT WHERE email = :email").param("email", email).query(::map).optional().orElse(null)

    fun findById(id: UUID): UserAccount? =
        jdbc.sql("$SELECT WHERE id = :id").param("id", id).query(::map).optional().orElse(null)

    /** Sirve en los dos contextos: al dar de alta un club y cuando un administrador suma personal. */
    fun insert(id: UUID, email: String, name: String, passwordHash: String, passwordChangeRequired: Boolean) {
        jdbc.sql(
            """
            INSERT INTO users (id, email, name, password_hash, password_change_required)
            VALUES (:id, :email, :name, :hash, :change)
            """,
        )
            .param("id", id)
            .param("email", email)
            .param("name", name)
            .param("hash", passwordHash)
            .param("change", passwordChangeRequired)
            .update()
    }

    fun updatePassword(id: UUID, passwordHash: String) {
        jdbc.sql("UPDATE users SET password_hash = :hash, password_change_required = false WHERE id = :id")
            .param("hash", passwordHash)
            .param("id", id)
            .update()
    }

    private fun map(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) = UserAccount(
        id = rs.getObject("id", UUID::class.java),
        email = rs.getString("email"),
        name = rs.getString("name"),
        passwordHash = rs.getString("password_hash"),
        passwordChangeRequired = rs.getBoolean("password_change_required"),
    )

    private companion object {
        const val SELECT = "SELECT id, email, name, password_hash, password_change_required FROM users"
    }
}
