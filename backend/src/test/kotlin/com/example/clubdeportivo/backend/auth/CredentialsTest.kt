package com.example.clubdeportivo.backend.auth

import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CredentialsTest {

    @Test
    fun claveDeFirma_faltanteInvalidaOCorta_frenaElArranque() {
        assertThatThrownBy { AuthConfiguration.signingKey("") }.hasMessageContaining("CLUB_AUTH_JWT_SECRET")
        assertThatThrownBy { AuthConfiguration.signingKey("no es base64!") }.hasMessageContaining("Base64")
        val corta = Base64.getEncoder().encodeToString(ByteArray(16))
        assertThatThrownBy { AuthConfiguration.signingKey(corta) }.hasMessageContaining("16 bytes")
    }

    @Test
    fun claveDeFirma_de32Bytes_seAcepta() {
        val clave = AuthConfiguration.signingKey(Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))

        assertThat(clave.algorithm).isEqualTo("HmacSHA256")
        assertThat(clave.encoded).hasSize(32)
    }

    @Test
    fun argon2id_verificaLaContraseñaCorrectaYRechazaLasDemas() {
        val hasher = PasswordHasher()

        val hash = hasher.hash("clave-segura-123")

        assertThat(hash).startsWith("\$argon2id\$v=19\$m=19456,t=2,p=1\$")
        assertThat(hasher.matches("clave-segura-123", hash)).isTrue()
        assertThat(hasher.matches("clave-segura-124", hash)).isFalse()
        assertThat(hasher.matchesNothing("clave-segura-123")).isFalse()
    }

    @Test
    fun tokensDeRenovacion_sonUnicosYSeGuardaSoloSuHash() {
        val tokens = List(100) { RefreshTokens.generate() }

        assertThat(tokens.toSet()).hasSize(100)
        assertThat(tokens.first()).hasSize(43)
        assertThat(RefreshTokens.hash(tokens.first())).hasSize(32).isEqualTo(RefreshTokens.hash(tokens.first()))
    }
}
