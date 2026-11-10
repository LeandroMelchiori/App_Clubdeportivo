package com.example.clubdeportivo.backend.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Responde todos los errores con el formato de detalles de problema (RFC 9457).
 *
 * Las excepciones propias de Spring MVC (ruta inexistente, método no permitido, cuerpo ilegible)
 * las resuelve [ResponseEntityExceptionHandler], con títulos tomados de `messages.properties`.
 * Cualquier otra excepción se responde como un error interno que no expone detalles al cliente.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ProblemDetail {
        log.error("Error no controlado en {} {}", request.method, request.requestURI, ex)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
        ).apply { title = "Error interno" }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
