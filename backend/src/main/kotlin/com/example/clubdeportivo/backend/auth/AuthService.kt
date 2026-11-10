package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.db.DatabaseContext
import com.example.clubdeportivo.backend.error.ClubAccessDeniedException
import com.example.clubdeportivo.backend.error.InvalidCredentialsException
import com.example.clubdeportivo.backend.error.InvalidRefreshTokenException
import com.example.clubdeportivo.backend.error.NoActiveClubsException
import com.example.clubdeportivo.backend.error.SamePasswordException
import com.example.clubdeportivo.backend.error.WrongCurrentPasswordException
import com.example.clubdeportivo.backend.user.UserAccount
import com.example.clubdeportivo.backend.user.UserRepository
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Ingreso, renovación y cierre de sesión. Todo corre en el contexto de identidad porque una cuenta
 * puede tener clubes distintos.
 *
 * Los logs registran ids de cuenta y de sesión, nunca emails, contraseñas ni IPs.
 */
@Service
class AuthService(
    private val db: DatabaseContext,
    private val users: UserRepository,
    private val sessions: SessionRepository,
    private val hasher: PasswordHasher,
    private val accessTokens: AccessTokenIssuer,
    private val properties: AuthProperties,
) {

    fun login(email: String, password: String, clubId: UUID?): AuthResponse {
        val account = Emails.normalize(email)
        // La verificación de Argon2 queda fuera de la transacción: tarda y no necesita la base.
        val user = db.identity { users.findByEmail(account) }
        val valid = if (user != null) hasher.matches(password, user.passwordHash) else hasher.matchesNothing(password)
        if (user == null || !valid) {
            log.info("Ingreso rechazado por credenciales inválidas")
            throw InvalidCredentialsException()
        }
        return db.identity {
            val clubs = sessions.activeClubsOf(user.id)
            if (clubs.isEmpty()) throw NoActiveClubsException()
            val chosen = if (clubId == null) clubs.first() else clubs.find { it.clubId == clubId }
            if (chosen == null) throw ClubAccessDeniedException()
            startSession(user, chosen, clubs)
        }
    }

    /**
     * Rota el token de renovación y, si se pide, cambia de club dentro de la misma sesión.
     *
     * Si llega un token que ya se usó, alguien lo copió: se revoca la sesión entera, así el token nuevo
     * que se emitió con él tampoco sirve.
     */
    fun refresh(refreshToken: String, clubId: UUID?): AuthResponse {
        val outcome = db.identity {
            val hash = RefreshTokens.hash(refreshToken)
            val stored = sessions.findRefreshTokenForUpdate(hash)
            when {
                stored == null || stored.sessionRevoked -> RefreshOutcome.Rejected
                stored.used -> {
                    sessions.revokeSession(stored.sessionId)
                    log.warn("Se reutilizó un token de renovación: sesión {} revocada", stored.sessionId)
                    RefreshOutcome.Rejected
                }
                stored.expired -> {
                    sessions.revokeSession(stored.sessionId)
                    RefreshOutcome.Rejected
                }
                else -> renew(hash, stored, clubId)
            }
        }
        return when (outcome) {
            is RefreshOutcome.Renewed -> outcome.response
            RefreshOutcome.Rejected -> throw InvalidRefreshTokenException()
            RefreshOutcome.ClubDenied -> throw ClubAccessDeniedException()
        }
    }

    /** Cierra la sesión del token. No dice si el token existía. */
    fun logout(refreshToken: String) {
        db.identity {
            sessions.findRefreshTokenForUpdate(RefreshTokens.hash(refreshToken))?.let { sessions.revokeSession(it.sessionId) }
        }
    }

    /**
     * Cambia la contraseña, cierra todas las sesiones de la cuenta en todos sus clubes y abre una nueva
     * en el club actual.
     */
    fun changePassword(principal: ClubPrincipal, currentPassword: String, newPassword: String): AuthResponse {
        val user = db.identity { users.findById(principal.userId) } ?: throw InvalidRefreshTokenException()
        if (!hasher.matches(currentPassword, user.passwordHash)) throw WrongCurrentPasswordException()
        if (currentPassword == newPassword) throw SamePasswordException()
        val newHash = hasher.hash(newPassword)
        return db.identity {
            val clubs = sessions.activeClubsOf(user.id)
            val current = clubs.find { it.clubId == principal.clubId } ?: throw ClubAccessDeniedException()
            users.updatePassword(user.id, newHash)
            sessions.revokeAllSessionsOf(user.id)
            log.info("Contraseña cambiada por la cuenta {}", user.id)
            startSession(user.copy(passwordChangeRequired = false), current, clubs)
        }
    }

    private fun renew(hash: ByteArray, stored: StoredRefreshToken, clubId: UUID?): RefreshOutcome {
        val user = users.findById(stored.userId) ?: return RefreshOutcome.Rejected
        val clubs = sessions.activeClubsOf(user.id)
        val target = clubs.find { it.clubId == (clubId ?: stored.clubId) }
        if (target == null) {
            // Pidió un club ajeno: no se toca la sesión. Si en cambio le dieron de baja del club de la
            // sesión, la sesión deja de servir.
            if (clubId != null && clubId != stored.clubId) return RefreshOutcome.ClubDenied
            sessions.revokeSession(stored.sessionId)
            return RefreshOutcome.Rejected
        }
        sessions.markRefreshTokenUsed(hash)
        if (target.clubId != stored.clubId) {
            sessions.switchClub(stored.sessionId, target.clubId)
            sessions.recordLogin(user.id, target.clubId)
        }
        return RefreshOutcome.Renewed(issueTokens(user, target, clubs, stored.sessionId))
    }

    private fun startSession(user: UserAccount, club: ClubAccess, clubs: List<ClubAccess>): AuthResponse {
        val sessionId = UUID.randomUUID()
        sessions.createSession(sessionId, user.id, club.clubId)
        sessions.recordLogin(user.id, club.clubId)
        log.info("Sesión {} abierta para la cuenta {} en el club {}", sessionId, user.id, club.clubId)
        return issueTokens(user, club, clubs, sessionId)
    }

    private fun issueTokens(user: UserAccount, club: ClubAccess, clubs: List<ClubAccess>, sessionId: UUID): AuthResponse {
        val refreshToken = RefreshTokens.generate()
        sessions.insertRefreshToken(RefreshTokens.hash(refreshToken), sessionId, properties.refreshTokenTtl)
        return AuthResponse(
            accessToken = accessTokens.issue(user.id, club, sessionId, user.passwordChangeRequired),
            tokenType = "Bearer",
            expiresIn = properties.accessTokenTtl.toSeconds(),
            refreshToken = refreshToken,
            refreshTokenExpiresIn = properties.refreshTokenTtl.toSeconds(),
            club = ClubAccessResponse(club),
            clubs = clubs.map(::ClubAccessResponse),
            passwordChangeRequired = user.passwordChangeRequired,
        )
    }

    private sealed interface RefreshOutcome {
        data class Renewed(val response: AuthResponse) : RefreshOutcome
        data object Rejected : RefreshOutcome
        data object ClubDenied : RefreshOutcome
    }

    private companion object {
        val log = LoggerFactory.getLogger(AuthService::class.java)
    }
}
