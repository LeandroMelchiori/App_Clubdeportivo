package com.example.clubdeportivo.backend.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

/**
 * Error de la API con su detalle de problema (RFC 9457). Lo responde [ApiExceptionHandler].
 *
 * Los textos están pensados para mostrarse a quien usa la app: nunca incluyen datos de otras
 * personas ni detalles internos.
 */
open class ApiProblemException(status: HttpStatus, title: String, detail: String) :
    ErrorResponseException(status, problem(status, title, detail), null)

private fun problem(status: HttpStatus, title: String, detail: String): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, detail).apply { this.title = title }

class InvalidCredentialsException : ApiProblemException(
    HttpStatus.UNAUTHORIZED,
    "Credenciales inválidas",
    "El email o la contraseña no son correctos.",
)

class InvalidRefreshTokenException : ApiProblemException(
    HttpStatus.UNAUTHORIZED,
    "Sesión inválida",
    "La sesión venció o se cerró. Hay que volver a ingresar.",
)

class ClubAccessDeniedException : ApiProblemException(
    HttpStatus.FORBIDDEN,
    "Sin acceso al club",
    "La cuenta no tiene una membresía activa en ese club.",
)

class NoActiveClubsException : ApiProblemException(
    HttpStatus.FORBIDDEN,
    "Sin clubes activos",
    "La cuenta no tiene una membresía activa en ningún club.",
)

class WrongCurrentPasswordException : ApiProblemException(
    HttpStatus.BAD_REQUEST,
    "Contraseña actual incorrecta",
    "La contraseña actual no es correcta.",
)

class SamePasswordException : ApiProblemException(
    HttpStatus.BAD_REQUEST,
    "Contraseña repetida",
    "La contraseña nueva tiene que ser distinta de la actual.",
)

class ConflictException(detail: String) : ApiProblemException(HttpStatus.CONFLICT, "Conflicto", detail)

class NotFoundException(detail: String) :
    ApiProblemException(HttpStatus.NOT_FOUND, "Recurso no encontrado", detail)
