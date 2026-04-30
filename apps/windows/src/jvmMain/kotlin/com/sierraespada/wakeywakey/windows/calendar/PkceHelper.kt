package com.sierraespada.wakeywakey.windows.calendar

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Genera los parámetros PKCE (RFC 7636) para OAuth 2.0 con apps instaladas.
 * Elimina la necesidad de client_secret en el flujo de escritorio.
 */
object PkceHelper {

    /** Genera un code_verifier aleatorio de 64 bytes (87 chars base64url). */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Calcula code_challenge = BASE64URL(SHA-256(code_verifier)). */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /** Genera un state aleatorio para protección CSRF. */
    fun generateState(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
