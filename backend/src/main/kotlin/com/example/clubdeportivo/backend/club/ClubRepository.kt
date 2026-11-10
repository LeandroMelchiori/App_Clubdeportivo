package com.example.clubdeportivo.backend.club

import com.example.clubdeportivo.backend.db.DatabaseContext
import java.util.UUID
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

data class Club(val id: UUID, val name: String)

@Repository
class ClubRepository(private val jdbc: JdbcClient, private val db: DatabaseContext) {

    /** Contexto de identidad: el club todavía no existe, así que no puede ser el club activo. */
    fun insert(id: UUID, name: String) {
        jdbc.sql("INSERT INTO clubs (id, name) VALUES (:id, :name)").param("id", id).param("name", name).update()
    }

    /** El club activo. Contexto de club. */
    fun current(): Club = jdbc.sql("SELECT id, name FROM clubs WHERE id = :club")
        .param("club", db.currentClubId())
        .query { rs, _ -> Club(rs.getObject("id", UUID::class.java), rs.getString("name")) }
        .single()
}
