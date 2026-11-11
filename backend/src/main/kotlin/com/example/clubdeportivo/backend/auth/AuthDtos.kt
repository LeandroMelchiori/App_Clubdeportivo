package com.example.clubdeportivo.backend.auth

import com.example.clubdeportivo.backend.club.Role
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

object PasswordPolicy {
    const val MIN_LENGTH = 10

    // Tope para que nadie mande contraseñas enormes y haga trabajar de más a Argon2.
    const val MAX_LENGTH = 128
    const val MESSAGE = "La contraseña tiene que tener entre 10 y 128 caracteres."
}

data class LoginRequest(
    @field:NotBlank(message = "El email es obligatorio.")
    @field:Size(max = 254, message = "El email no puede superar los 254 caracteres.")
    val email: String = "",
    @field:NotBlank(message = "La contraseña es obligatoria.")
    @field:Size(max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.MESSAGE)
    val password: String = "",
    @field:Schema(description = "Club con el que operar. Si se omite, se usa el del último ingreso.")
    val clubId: UUID? = null,
)

data class RefreshRequest(
    @field:NotBlank(message = "El token de renovación es obligatorio.")
    val refreshToken: String = "",
    @field:Schema(description = "Club al que pasar. Si se omite, se sigue en el mismo.")
    val clubId: UUID? = null,
)

data class LogoutRequest(
    @field:NotBlank(message = "El token de renovación es obligatorio.")
    val refreshToken: String = "",
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "La contraseña actual es obligatoria.")
    @field:Size(max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.MESSAGE)
    val currentPassword: String = "",
    @field:Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.MESSAGE)
    val newPassword: String = "",
)

data class ClubAccessResponse(val id: UUID, val name: String, val role: Role) {
    constructor(access: ClubAccess) : this(access.clubId, access.clubName, access.role)
}

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    @field:Schema(description = "Segundos de vida del token de acceso.")
    val expiresIn: Long,
    @field:Schema(description = "Token opaco de un solo uso: cada renovación devuelve uno nuevo.")
    val refreshToken: String,
    val refreshTokenExpiresIn: Long,
    @field:Schema(description = "Club activo, el del token de acceso.")
    val club: ClubAccessResponse,
    @field:Schema(description = "Todos los clubes de la cuenta, para elegir otro con /auth/refresh.")
    val clubs: List<ClubAccessResponse>,
    @field:Schema(description = "Si es true, el token solo sirve para cambiar la contraseña temporal.")
    val passwordChangeRequired: Boolean,
)
