package com.sierraespada.wakeywakey.windows.calendar

import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokens(
    val accessToken:  String,
    val refreshToken: String,
    val expiresAt:    Long,     // epoch millis
    val provider:     String,   // "google" | "microsoft"
    val accountEmail: String = "",
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt - REFRESH_MARGIN_MS

    companion object {
        /** Refresca el token 5 min antes de que expire. */
        private const val REFRESH_MARGIN_MS = 5 * 60_000L
    }
}
