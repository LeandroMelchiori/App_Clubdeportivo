package com.example.clubdeportivo.backend.club

import com.example.clubdeportivo.backend.auth.ClubPrincipal
import com.example.clubdeportivo.backend.auth.Emails
import com.example.clubdeportivo.backend.auth.PasswordHasher
import com.example.clubdeportivo.backend.auth.SessionRepository
import com.example.clubdeportivo.backend.db.DatabaseContext
import com.example.clubdeportivo.backend.error.ConflictException
import com.example.clubdeportivo.backend.user.UserRepository
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class ClubService(
    private val db: DatabaseContext,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val sessions: SessionRepository,
    private val hasher: PasswordHasher,
) {

    /**
     * Da de alta un club con su administrador. Si el email ya tiene cuenta, se rechaza: para sumarse a otro
     * club, el administrador de ese club la agrega como personal.
     */
    fun create(request: CreateClubRequest): CreateClubResponse {
        val email = Emails.normalize(request.adminEmail)
        val passwordHash = hasher.hash(request.adminPassword)
        val clubId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        try {
            db.identity {
                if (users.findByEmail(email) != null) throw emailInUse()
                clubs.insert(clubId, request.clubName.trim())
                users.insert(userId, email, request.adminName.trim(), passwordHash, passwordChangeRequired = false)
                sessions.insertMembership(userId, clubId, Role.ADMIN)
            }
        } catch (e: DuplicateKeyException) {
            // Otro alta con el mismo email ganó la carrera entre la consulta y el insert.
            throw emailInUse()
        }
        log.info("Club {} dado de alta con la cuenta administradora {}", clubId, userId)
        return CreateClubResponse(clubId = clubId, clubName = request.clubName.trim(), adminUserId = userId)
    }

    fun current(principal: ClubPrincipal): CurrentClubResponse {
        val club = db.inClub(principal.clubId) { clubs.current() }
        return CurrentClubResponse(id = club.id, name = club.name, role = principal.role)
    }

    private fun emailInUse() = ConflictException("Ya existe una cuenta con ese email.")

    private companion object {
        val log = LoggerFactory.getLogger(ClubService::class.java)
    }
}
