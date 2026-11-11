package com.example.clubdeportivo.backend.security

import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component

@ConfigurationProperties("club.network")
data class NetworkProperties(
    /** Rangos CIDR de los proxies de confianza. Solo a ellos se les cree el encabezado X-Forwarded-For. */
    val trustedProxies: List<String> = emptyList(),
)

/**
 * Averigua la IP del cliente para el límite de intentos de ingreso.
 *
 * Detrás de Traefik, la conexión llega desde el proxy y la IP real viene en X-Forwarded-For. Ese
 * encabezado lo puede escribir cualquiera, así que solo se usa cuando la conexión viene de un proxy de
 * confianza. Se recorre de derecha a izquierda, porque cada proxy agrega al final la IP que vio, y se
 * toma la primera que no es de un proxy de confianza. Si el encabezado trae algo que no es una IP, se
 * descarta entero y se usa la IP de la conexión.
 */
@Component
class ClientIpResolver(properties: NetworkProperties) {

    private val trustedProxies = properties.trustedProxies.map(String::trim).filter(String::isNotEmpty).map(::IpAddressMatcher)

    fun resolve(request: HttpServletRequest): String {
        val remote = request.remoteAddr
        if (!isTrustedProxy(remote)) return remote
        val forwarded = request.getHeaders(FORWARDED_FOR).toList()
            .flatMap { it.split(',') }
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (forwarded.isEmpty() || !forwarded.all(::isIpLiteral)) return remote
        return forwarded.asReversed().firstOrNull { !isTrustedProxy(it) } ?: forwarded.first()
    }

    private fun isTrustedProxy(ip: String): Boolean = isIpLiteral(ip) && trustedProxies.any { it.matches(ip) }

    private companion object {
        const val FORWARDED_FOR = "X-Forwarded-For"

        private val IPV4 = Regex("""^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$""")
        private val IPV6_CHARS = Regex("""^[0-9A-Fa-f:.]{2,45}$""")

        // IpAddressMatcher resuelve nombres por DNS: antes de pasarle algo hay que asegurarse de que es una IP.
        // Con ':' Java lo interpreta como IPv6 literal y no consulta DNS; si no lo es (por ejemplo
        // "198.51.100.1:443"), falla y se descarta.
        fun isIpLiteral(value: String): Boolean = IPV4.matches(value) ||
            (':' in value && IPV6_CHARS.matches(value) && runCatching { InetAddress.getByName(value) }.isSuccess)
    }
}
