package com.example.clubdeportivo.backend.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpResolverTest {

    private val resolver = ClientIpResolver(NetworkProperties(trustedProxies = listOf("172.18.0.0/16")))

    private fun request(remote: String, vararg forwardedFor: String) = MockHttpServletRequest().apply {
        remoteAddr = remote
        forwardedFor.forEach { addHeader("X-Forwarded-For", it) }
    }

    @Test
    fun desdeAfueraDelProxy_seIgnoraElEncabezado() {
        assertThat(resolver.resolve(request("203.0.113.7", "198.51.100.1"))).isEqualTo("203.0.113.7")
    }

    @Test
    fun desdeElProxy_seUsaLaIpQueInforma() {
        assertThat(resolver.resolve(request("172.18.0.2", "198.51.100.1"))).isEqualTo("198.51.100.1")
    }

    @Test
    fun desdeElProxy_unaIpFalsificadaPorElClienteQuedaALaIzquierdaYNoSeUsa() {
        // Si un proxy agregara la IP real al final de lo que mandó el cliente, la buena es la de la derecha.
        assertThat(resolver.resolve(request("172.18.0.2", "10.9.9.9, 198.51.100.1"))).isEqualTo("198.51.100.1")
        assertThat(resolver.resolve(request("172.18.0.2", "10.9.9.9", "198.51.100.1"))).isEqualTo("198.51.100.1")
    }

    @Test
    fun proxiesDeConfianzaEncadenados_seSaltean() {
        assertThat(resolver.resolve(request("172.18.0.2", "198.51.100.1, 172.18.0.9"))).isEqualTo("198.51.100.1")
    }

    @Test
    fun desdeElProxy_sinEncabezadoOConValoresQueNoSonIps_seUsaLaConexion() {
        assertThat(resolver.resolve(request("172.18.0.2"))).isEqualTo("172.18.0.2")
        assertThat(resolver.resolve(request("172.18.0.2", "198.51.100.1, ejemplo.com"))).isEqualTo("172.18.0.2")
        assertThat(resolver.resolve(request("172.18.0.2", "198.51.100.1:443"))).isEqualTo("172.18.0.2")
    }

    @Test
    fun ipv6_seAcepta() {
        assertThat(resolver.resolve(request("172.18.0.2", "2001:db8::7"))).isEqualTo("2001:db8::7")
    }

    @Test
    fun sinProxiesConfigurados_nuncaSeUsaElEncabezado() {
        val sinProxies = ClientIpResolver(NetworkProperties())

        assertThat(sinProxies.resolve(request("172.18.0.2", "198.51.100.1"))).isEqualTo("172.18.0.2")
    }
}
