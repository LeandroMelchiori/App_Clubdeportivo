package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.error.TooManyAttemptsException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class LoginAttemptLimiterTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }

    private val clock = MutableClock(Instant.parse("2026-11-10T12:00:00Z"))
    private val limits = AuthProperties.LoginLimits(maxFailuresPerAccount = 3, maxFailuresPerIp = 5, window = Duration.ofMinutes(15))
    private val limiter = LoginAttemptLimiter(AuthProperties(loginLimits = limits), clock)

    @Test
    fun cuenta_seBloqueaAlLlegarAlLimite_hastaQueTerminaLaVentana() {
        repeat(3) { limiter.recordFailure("ana@club.com", "10.0.0.1") }
        clock.now = clock.now.plus(Duration.ofMinutes(5))

        val bloqueo = catchThrowable { limiter.checkAllowed("ana@club.com", "10.0.0.2") }
        assertThat(bloqueo).isInstanceOf(TooManyAttemptsException::class.java)
        bloqueo as TooManyAttemptsException
        assertThat(bloqueo.headers.getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600")

        clock.now = clock.now.plus(Duration.ofMinutes(10))
        assertThatCode { limiter.checkAllowed("ana@club.com", "10.0.0.2") }.doesNotThrowAnyException()
    }

    @Test
    fun ingresoCorrecto_reiniciaLaCuenta_peroNoLaIp() {
        repeat(2) { limiter.recordFailure("ana@club.com", "10.0.0.1") }
        limiter.recordSuccess("ana@club.com")
        repeat(2) { limiter.recordFailure("ana@club.com", "10.0.0.1") }

        assertThatCode { limiter.checkAllowed("ana@club.com", "10.0.0.9") }.doesNotThrowAnyException()
        limiter.recordFailure("otra@club.com", "10.0.0.1")
        assertThatThrownBy { limiter.checkAllowed("nueva@club.com", "10.0.0.1") }
            .isInstanceOf(TooManyAttemptsException::class.java)
    }

    @Test
    fun ip_seBloqueaAunqueCadaIntentoSeaConOtraCuenta() {
        repeat(5) { limiter.recordFailure("cuenta$it@club.com", "10.0.0.1") }

        assertThatThrownBy { limiter.checkAllowed("cuenta-nueva@club.com", "10.0.0.1") }
            .isInstanceOf(TooManyAttemptsException::class.java)
        assertThatCode { limiter.checkAllowed("cuenta-nueva@club.com", "10.0.0.2") }.doesNotThrowAnyException()
    }

    @Test
    fun falloDespuesDeLaVentana_empiezaUnaVentanaNueva() {
        repeat(2) { limiter.recordFailure("ana@club.com", "10.0.0.1") }
        clock.now = clock.now.plus(Duration.ofMinutes(16))
        repeat(2) { limiter.recordFailure("ana@club.com", "10.0.0.1") }

        assertThatCode { limiter.checkAllowed("ana@club.com", "10.0.0.3") }.doesNotThrowAnyException()
    }
}
