package com.example.clubdeportivo.backend.club

import com.example.clubdeportivo.backend.auth.ClubPrincipal
import com.example.clubdeportivo.backend.auth.PasswordPolicy
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class CreateClubRequest(
    @field:NotBlank(message = "El nombre del club es obligatorio.")
    @field:Size(max = 100, message = "El nombre del club no puede superar los 100 caracteres.")
    val clubName: String = "",
    @field:NotBlank(message = "El nombre de quien administra es obligatorio.")
    @field:Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    val adminName: String = "",
    @field:NotBlank(message = "El email es obligatorio.")
    @field:Email(message = "El email no tiene un formato válido.")
    @field:Size(max = 254, message = "El email no puede superar los 254 caracteres.")
    val adminEmail: String = "",
    @field:Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH, message = PasswordPolicy.MESSAGE)
    val adminPassword: String = "",
)

data class CreateClubResponse(val clubId: UUID, val clubName: String, val adminUserId: UUID)

data class CurrentClubResponse(val id: UUID, val name: String, val role: Role)

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Clubes")
class ClubController(private val clubs: ClubService) {

    @PostMapping("/clubs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dar de alta un club", description = "Crea el club y la cuenta de su administrador.")
    fun create(@Valid @RequestBody body: CreateClubRequest): CreateClubResponse = clubs.create(body)

    @GetMapping("/club")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Club activo", description = "El club del token de acceso y el rol en él.")
    fun current(@AuthenticationPrincipal principal: ClubPrincipal): CurrentClubResponse = clubs.current(principal)
}
