package com.example.clubdeportivo.backend.auth

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import java.time.Clock
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

@Configuration(proxyBeanMethods = false)
class AuthConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun jwtSigningKey(properties: AuthProperties): SecretKey = signingKey(properties.jwtSecret)

    @Bean
    fun jwtEncoder(key: SecretKey): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))

    @Bean
    fun jwtDecoder(key: SecretKey, properties: AuthProperties): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().apply {
            setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer))
        }

    companion object {
        private const val MIN_KEY_BYTES = 32

        /** Valida la clave al arrancar, así una clave faltante o corta frena el inicio con un mensaje claro. */
        fun signingKey(base64Secret: String): SecretKey {
            check(base64Secret.isNotBlank()) {
                "Falta la clave de firma de los tokens: definir CLUB_AUTH_JWT_SECRET con 32 bytes o más en Base64."
            }
            val bytes = try {
                Base64.getDecoder().decode(base64Secret.trim())
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException("CLUB_AUTH_JWT_SECRET no es Base64 válido.", e)
            }
            check(bytes.size >= MIN_KEY_BYTES) {
                "CLUB_AUTH_JWT_SECRET tiene ${bytes.size} bytes; hacen falta $MIN_KEY_BYTES o más."
            }
            return SecretKeySpec(bytes, "HmacSHA256")
        }
    }
}
