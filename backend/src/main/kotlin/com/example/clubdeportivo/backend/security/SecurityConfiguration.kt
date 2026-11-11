package com.example.clubdeportivo.backend.security

import com.example.clubdeportivo.backend.club.Role
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * API sin estado: cada pedido trae su token de acceso. El club y el rol salen siempre del token.
 *
 * Lo que no está listado como público pide un rol del club activo, así una ruta nueva nace protegida.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity, problems: ProblemDetailSecurityHandlers): SecurityFilterChain {
        val anyRole = Role.entries.map { it.name }.toTypedArray()
        http {
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/error", permitAll)
                authorize("/api/v1/health", permitAll)
                authorize("/api/v1/health/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/info", permitAll)
                authorize("/api/v1/openapi", permitAll)
                authorize("/api/v1/openapi/**", permitAll)
                authorize("/api/v1/docs", permitAll)
                authorize("/api/v1/swagger-ui/**", permitAll)
                authorize(HttpMethod.POST, "/api/v1/clubs", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/login", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/refresh", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/logout", permitAll)
                // Es la única ruta que acepta un token con la contraseña temporal pendiente de cambio.
                authorize(HttpMethod.POST, "/api/v1/auth/password", authenticated)
                authorize("/api/v1/members/**", hasRole(Role.ADMIN.name))
                authorize(anyRequest, hasAnyRole(*anyRole))
            }
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = ClubJwtAuthenticationConverter() }
                authenticationEntryPoint = problems
                accessDeniedHandler = problems
            }
            exceptionHandling {
                authenticationEntryPoint = problems
                accessDeniedHandler = problems
            }
        }
        return http.build()
    }
}
