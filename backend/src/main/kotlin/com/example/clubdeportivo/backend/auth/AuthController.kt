package com.example.clubdeportivo.backend.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
class AuthController(private val auth: AuthService) {

    @PostMapping("/login")
    @Operation(
        summary = "Ingresar con email y contraseña",
        description = "Abre una sesión en el club pedido o, si no se indica, en el del último ingreso. " +
            "La respuesta lista todos los clubes de la cuenta.",
    )
    fun login(@Valid @RequestBody body: LoginRequest): AuthResponse = auth.login(body.email, body.password, body.clubId)

    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar la sesión",
        description = "Devuelve un token de acceso y un token de renovación nuevos; el anterior deja de servir. " +
            "Con clubId pasa la sesión a otro club de la cuenta.",
    )
    fun refresh(@Valid @RequestBody body: RefreshRequest): AuthResponse = auth.refresh(body.refreshToken, body.clubId)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar la sesión", description = "Revoca la sesión del token de renovación.")
    fun logout(@Valid @RequestBody body: LogoutRequest) = auth.logout(body.refreshToken)

    @PostMapping("/password")
    @SecurityRequirement(name = "bearer")
    @Operation(
        summary = "Cambiar la contraseña",
        description = "Cierra todas las sesiones de la cuenta y abre una nueva en el club actual.",
    )
    fun changePassword(
        @Valid @RequestBody body: ChangePasswordRequest,
        @AuthenticationPrincipal principal: ClubPrincipal,
    ): AuthResponse = auth.changePassword(principal, body.currentPassword, body.newPassword)
}
