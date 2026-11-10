package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.club.Role
import java.time.Clock
import java.util.UUID
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component

/** Nombres de los claims propios del token de acceso. */
object AccessTokenClaims {
    const val CLUB = "club"
    const val ROLE = "role"
    const val SESSION = "sid"
    const val PASSWORD_CHANGE_REQUIRED = "pwd_change"
}

/**
 * Firma los tokens de acceso (JWT con HS256). Llevan la cuenta, el club activo, el rol en ese club, la
 * sesión y si falta cambiar la contraseña temporal. No llevan email ni otros datos personales.
 */
@Component
class AccessTokenIssuer(
    private val encoder: JwtEncoder,
    private val properties: AuthProperties,
    private val clock: Clock,
) {
    fun issue(userId: UUID, access: ClubAccess, sessionId: UUID, passwordChangeRequired: Boolean): String {
        val now = clock.instant()
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now + properties.accessTokenTtl)
            .claim(AccessTokenClaims.CLUB, access.clubId.toString())
            .claim(AccessTokenClaims.ROLE, access.role.name)
            .claim(AccessTokenClaims.SESSION, sessionId.toString())
            .claim(AccessTokenClaims.PASSWORD_CHANGE_REQUIRED, passwordChangeRequired)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}

/** Lo que la API sabe de quien llama, sacado del token de acceso. */
data class ClubPrincipal(
    val userId: UUID,
    val clubId: UUID,
    val role: Role,
    val sessionId: UUID,
    val passwordChangeRequired: Boolean,
)
