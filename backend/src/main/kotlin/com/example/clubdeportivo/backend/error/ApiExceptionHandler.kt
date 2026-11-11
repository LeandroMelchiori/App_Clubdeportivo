package com.example.clubdeportivo.backend.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Responde todos los errores con el formato de detalles de problema (RFC 9457).
 *
 * Las excepciones propias de Spring MVC (ruta inexistente, método no permitido, cuerpo ilegible)
 * y las [ApiProblemException] las resuelve [ResponseEntityExceptionHandler], con títulos tomados de
 * `messages.properties`. Cualquier otra excepción se responde como un error interno que no expone
 * detalles al cliente.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        // Solo el campo y el mensaje: el valor rechazado puede ser una contraseña o un dato personal.
        val errors = ex.bindingResult.fieldErrors.map { mapOf("field" to it.field, "message" to it.defaultMessage) }
        val problem = ProblemDetail.forStatusAndDetail(status, "Hay campos con valores inválidos.").apply {
            title = "Datos inválidos"
            setProperty("errors", errors)
        }
        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ProblemDetail {
        // El mensaje de la excepción puede traer datos de la consulta (un email, un DNI): se registra
        // solo el tipo y dónde ocurrió. La traza completa queda en el nivel DEBUG.
        log.error(
            "Error no controlado en {} {}: {} en {}",
            request.method,
            request.requestURI,
            ex.javaClass.name,
            ex.stackTrace.firstOrNull(),
        )
        log.debug("Traza del error no controlado", ex)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
        ).apply { title = "Error interno" }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
