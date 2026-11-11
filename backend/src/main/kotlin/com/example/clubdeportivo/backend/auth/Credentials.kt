package com.example.clubdeportivo.backend.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Component

/** Hash de contraseñas con Argon2id, con los parámetros mínimos que recomienda OWASP (19 MiB, 2 pasadas). */
@Component
class PasswordHasher {

    private val encoder = Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2)

    // Cuando el email no existe se verifica contra este hash igual, para que la respuesta tarde lo
    // mismo y no delate qué cuentas existen.
    private val dummyHash = encoder.encode("contraseña de relleno para igualar tiempos")

    fun hash(rawPassword: String): String = checkNotNull(encoder.encode(rawPassword))

    fun matches(rawPassword: String, hash: String): Boolean = encoder.matches(rawPassword, hash)

    fun matchesNothing(rawPassword: String): Boolean {
        encoder.matches(rawPassword, dummyHash)
        return false
    }
}

object Emails {
    fun normalize(email: String): String = email.trim().lowercase(Locale.ROOT)
}

/** Tokens de renovación: 256 bits al azar. En la base solo se guarda su SHA-256. */
object RefreshTokens {
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
}

/** Contraseñas temporales para el personal: sin caracteres que se confundan al dictarlas (0, O, 1, l, I). */
object TemporaryPasswords {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private const val LENGTH = 12
    private val random = SecureRandom()

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }
}
