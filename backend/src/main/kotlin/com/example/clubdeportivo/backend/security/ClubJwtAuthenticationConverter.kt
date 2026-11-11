package com.example.clubdeportivo.backend.security

import com.example.clubdeportivo.backend.auth.AccessTokenClaims
import com.example.clubdeportivo.backend.auth.ClubPrincipal
import com.example.clubdeportivo.backend.club.Role
import java.util.UUID
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException

/** Autenticación de un pedido con token de acceso válido. El principal es un [ClubPrincipal]. */
class ClubAuthentication(private val principal: ClubPrincipal, private val jwt: Jwt, authorities: Collection<GrantedAuthority>) :
    AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getPrincipal(): ClubPrincipal = principal

    override fun getCredentials(): Jwt = jwt
}

/**
 * Pasa los claims del token a un [ClubPrincipal]. El rol del club se convierte en `ROLE_<rol>`, salvo
 * que falte cambiar la contraseña temporal: en ese caso no se da ningún rol y el token solo sirve para
 * las rutas que piden estar autenticado, como el cambio de contraseña.
 */
class ClubJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val principal = try {
            ClubPrincipal(
                userId = UUID.fromString(requireNotNull(jwt.subject)),
                clubId = UUID.fromString(requireNotNull(jwt.getClaimAsString(AccessTokenClaims.CLUB))),
                role = Role.valueOf(requireNotNull(jwt.getClaimAsString(AccessTokenClaims.ROLE))),
                sessionId = UUID.fromString(requireNotNull(jwt.getClaimAsString(AccessTokenClaims.SESSION))),
                passwordChangeRequired = jwt.getClaim<Boolean>(AccessTokenClaims.PASSWORD_CHANGE_REQUIRED) ?: true,
            )
        } catch (e: RuntimeException) {
            throw InvalidBearerTokenException("El token no tiene los datos esperados", e)
        }
        val authorities =
            if (principal.passwordChangeRequired) emptyList() else listOf(SimpleGrantedAuthority("ROLE_${principal.role.name}"))
        return ClubAuthentication(principal, jwt, authorities)
    }
}
