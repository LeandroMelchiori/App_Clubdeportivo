package com.example.clubdeportivo.backend.error

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class ApiExceptionHandlerTest {

    // Al estar anidado en una clase de test, el escaneo de componentes de @SpringBootTest lo ignora.
    @RestController
    class ControladorQueFalla {
        @GetMapping("/falla")
        fun falla(): String = throw IllegalStateException("detalle interno que no debe salir")
    }

    private val mockMvc = MockMvcBuilders.standaloneSetup(ControladorQueFalla())
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun errorInesperado_respondeProblemDetailSinDetallesInternos() {
        mockMvc.get("/falla").andExpect {
            status { isInternalServerError() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(500) }
            jsonPath("$.title") { value("Error interno") }
            jsonPath("$.instance") { value("/falla") }
            content { string(not(containsString("detalle interno"))) }
        }
    }
}
