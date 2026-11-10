package com.example.clubdeportivo.backend.member

import com.example.clubdeportivo.backend.auth.ClubPrincipal
import com.example.clubdeportivo.backend.auth.Emails
import com.example.clubdeportivo.backend.auth.PasswordHasher
import com.example.clubdeportivo.backend.auth.TemporaryPasswords
import com.example.clubdeportivo.backend.db.DatabaseContext
import com.example.clubdeportivo.backend.error.ConflictException
import com.example.clubdeportivo.backend.error.NotFoundException
import com.example.clubdeportivo.backend.user.UserRepository
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

/** Administración del personal del club activo. Todo corre en el contexto de club. */
@Service
class MemberService(
    private val db: DatabaseContext,
    private val members: MemberRepository,
    private val users: UserRepository,
    private val hasher: PasswordHasher,
) {

    fun list(principal: ClubPrincipal): List<MemberResponse> =
        db.inClub(principal.clubId) { members.list() }.map(::MemberResponse)

    /**
     * Suma a una persona al club.
     *
     * - Si el email no tiene cuenta, la crea con una contraseña temporal que se devuelve una sola vez y que
     *   hay que cambiar en el primer ingreso.
     * - Si ya tiene cuenta en otro club, le agrega este club con el rol pedido y conserva su contraseña.
     * - Si había sido dada de baja de este club, la reactiva con el rol pedido.
     */
    fun add(principal: ClubPrincipal, request: AddMemberRequest): AddMemberResponse {
        val email = Emails.normalize(request.email)
        val role = checkNotNull(request.role)
        val existingId = db.inClub(principal.clubId) { members.userIdByEmail(email) }
        // Argon2 tarda: el hash se calcula antes de abrir la transacción.
        val temporaryPassword = if (existingId == null) TemporaryPasswords.generate() else null
        val temporaryHash = temporaryPassword?.let(hasher::hash)
        val member = try {
            db.inClub(principal.clubId) {
                val userId = existingId ?: UUID.randomUUID()
                if (existingId == null) {
                    users.insert(userId, email, request.name.trim(), checkNotNull(temporaryHash), passwordChangeRequired = true)
                }
                val current = members.find(userId)
                when {
                    current == null -> members.insert(userId, role)
                    current.active -> throw ConflictException("Esa persona ya es miembro del club.")
                    else -> members.reactivate(userId, role)
                }
                checkNotNull(members.find(userId))
            }
        } catch (e: DuplicateKeyException) {
            // Otra alta con el mismo email ganó la carrera.
            throw ConflictException("Esa persona ya es miembro del club.")
        }
        log.info("La cuenta {} sumó a la cuenta {} al club {} como {}", principal.userId, member.userId, principal.clubId, role)
        return AddMemberResponse(
            member = MemberResponse(member),
            temporaryPassword = temporaryPassword,
            existingAccount = existingId != null,
        )
    }

    /** Da de baja a una persona del club y cierra sus sesiones en él. */
    fun remove(principal: ClubPrincipal, userId: UUID) {
        if (userId == principal.userId) throw ConflictException("No es posible darse de baja a uno mismo.")
        db.inClub(principal.clubId) {
            if (!members.deactivate(userId)) throw NotFoundException("Esa persona no es miembro activo del club.")
            members.revokeSessions(userId)
        }
        log.info("La cuenta {} dio de baja a la cuenta {} del club {}", principal.userId, userId, principal.clubId)
    }

    private companion object {
        val log = LoggerFactory.getLogger(MemberService::class.java)
    }
}
