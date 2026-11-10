package com.example.clubdeportivo.backend.member

import com.example.clubdeportivo.backend.auth.ClubPrincipal
import com.example.clubdeportivo.backend.club.Role
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class AddMemberRequest(
    @field:NotBlank(message = "El nombre es obligatorio.")
    @field:Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    val name: String = "",
    @field:NotBlank(message = "El email es obligatorio.")
    @field:Email(message = "El email no tiene un formato válido.")
    @field:Size(max = 254, message = "El email no puede superar los 254 caracteres.")
    val email: String = "",
    @field:NotNull(message = "El rol es obligatorio.")
    val role: Role? = null,
)

data class MemberResponse(val userId: UUID, val name: String, val email: String, val role: Role, val active: Boolean) {
    constructor(member: Member) : this(member.userId, member.name, member.email, member.role, member.active)
}

data class AddMemberResponse(
    val member: MemberResponse,
    @field:Schema(description = "Contraseña temporal para el primer ingreso. Solo viene cuando se creó la cuenta.")
    val temporaryPassword: String?,
    @field:Schema(description = "True si el email ya tenía cuenta en otro club: conserva su contraseña.")
    val existingAccount: Boolean,
)

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Personal del club")
@SecurityRequirement(name = "bearer")
class MemberController(private val members: MemberService) {

    @GetMapping
    @Operation(summary = "Listar el personal del club activo", description = "Solo para administración.")
    fun list(@AuthenticationPrincipal principal: ClubPrincipal): List<MemberResponse> = members.list(principal)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Sumar a una persona al club activo",
        description = "Solo para administración. Si el email no tiene cuenta, se crea con una contraseña temporal.",
    )
    fun add(@Valid @RequestBody body: AddMemberRequest, @AuthenticationPrincipal principal: ClubPrincipal): AddMemberResponse =
        members.add(principal, body)

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dar de baja a una persona del club activo", description = "Cierra sus sesiones en este club.")
    fun remove(@PathVariable userId: UUID, @AuthenticationPrincipal principal: ClubPrincipal) =
        members.remove(principal, userId)
}
