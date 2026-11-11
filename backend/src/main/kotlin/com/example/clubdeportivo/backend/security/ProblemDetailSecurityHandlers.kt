package com.example.clubdeportivo.backend.security

import com.example.clubdeportivo.backend.auth.ClubPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/** Responde los 401 y 403 de Spring Security con el mismo formato de problema que el resto de la API. */
@Component
class ProblemDetailSecurityHandlers(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint, AccessDeniedHandler {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        write(request, response, HttpStatus.UNAUTHORIZED, "No autenticado", "Hace falta un token de acceso válido y vigente.")
    }

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? ClubPrincipal
        val detail = if (principal?.passwordChangeRequired == true) {
            "Antes de continuar hay que cambiar la contraseña temporal."
        } else {
            "El rol en este club no permite esta operación."
        }
        write(request, response, HttpStatus.FORBIDDEN, "Acceso denegado", detail)
    }

    private fun write(request: HttpServletRequest, response: HttpServletResponse, status: HttpStatus, title: String, detail: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val body = mapOf("title" to title, "status" to status.value(), "detail" to detail, "instance" to request.requestURI)
        jsonMapper.writeValue(response.outputStream, body)
    }
}
