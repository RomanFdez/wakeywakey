package com.sierraespada.wakeywakey.windows.calendar

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persiste los tokens OAuth en ~/.wakeywakey/tokens/.
 * Directorio creado automáticamente si no existe.
 */
object TokenStorage {

    private val dir = File(System.getProperty("user.home"), ".wakeywakey/tokens")
        .also { it.mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }

    fun save(tokens: OAuthTokens) {
        File(dir, "${tokens.provider}.json").writeText(json.encodeToString(tokens))
    }

    fun load(provider: String): OAuthTokens? = runCatching {
        json.decodeFromString<OAuthTokens>(File(dir, "$provider.json").readText())
    }.getOrNull()

    fun clear(provider: String) {
        File(dir, "$provider.json").delete()
    }

    fun hasAny(): Boolean =
        listOf("google", "microsoft").any { File(dir, "$it.json").exists() }

    fun loadAny(): OAuthTokens? =
        load("google") ?: load("microsoft")
}
