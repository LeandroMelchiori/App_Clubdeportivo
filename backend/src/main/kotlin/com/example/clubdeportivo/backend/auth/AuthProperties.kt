package com.example.clubdeportivo.backend.auth

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("club.auth")
data class AuthProperties(
    /** Clave de firma de los tokens de acceso, en Base64 y de 32 bytes o más. Viene del entorno. */
    val jwtSecret: String = "",
    val issuer: String = "club-deportivo-api",
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(30),
    val loginLimits: LoginLimits = LoginLimits(),
) {
    /** Intentos fallidos permitidos dentro de cada ventana, por cuenta y por IP. */
    data class LoginLimits(
        val maxFailuresPerAccount: Int = 5,
        val maxFailuresPerIp: Int = 20,
        val window: Duration = Duration.ofMinutes(15),
    )
}
