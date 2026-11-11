package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.error.TooManyAttemptsException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

/**
 * Limita los intentos fallidos de ingreso por cuenta y por IP, con una ventana fija que arranca en el
 * primer fallo.
 *
 * La cuenta se identifica por el email normalizado, exista o no, así el límite no delata qué cuentas
 * existen. Un ingreso correcto reinicia el contador de la cuenta, pero no el de la IP.
 *
 * Los contadores viven en memoria: alcanza para una sola instancia de la API y se reinician con ella.
 */
@Component
class LoginAttemptLimiter(properties: AuthProperties, private val clock: Clock) {

    private data class Window(val start: Instant, val failures: Int)

    private class Counter(val maxFailures: Int, val window: Duration) {
        val windows = ConcurrentHashMap<String, Window>()

        fun blockedFor(key: String, now: Instant): Duration? {
            val current = windows[key] ?: return null
            val end = current.start + window
            return if (current.failures >= maxFailures && now < end) Duration.between(now, end) else null
        }

        fun recordFailure(key: String, now: Instant) {
            if (windows.size > MAX_TRACKED_KEYS) windows.values.removeIf { now >= it.start + window }
            windows.compute(key) { _, current ->
                if (current == null || now >= current.start + window) Window(now, 1)
                else current.copy(failures = current.failures + 1)
            }
        }
    }

    private val accounts = Counter(properties.loginLimits.maxFailuresPerAccount, properties.loginLimits.window)
    private val ips = Counter(properties.loginLimits.maxFailuresPerIp, properties.loginLimits.window)

    /** Lanza [TooManyAttemptsException] si la cuenta o la IP agotaron sus intentos. */
    fun checkAllowed(account: String, ip: String) {
        val now = clock.instant()
        val wait = listOfNotNull(accounts.blockedFor(account, now), ips.blockedFor(ip, now)).maxOrNull()
        if (wait != null) throw TooManyAttemptsException(wait)
    }

    fun recordFailure(account: String, ip: String) {
        val now = clock.instant()
        accounts.recordFailure(account, now)
        ips.recordFailure(ip, now)
    }

    fun recordSuccess(account: String) {
        accounts.windows.remove(account)
    }

    /** Solo para tests: todos comparten la IP local. */
    fun clear() {
        accounts.windows.clear()
        ips.windows.clear()
    }

    private companion object {
        const val MAX_TRACKED_KEYS = 10_000
    }
}
