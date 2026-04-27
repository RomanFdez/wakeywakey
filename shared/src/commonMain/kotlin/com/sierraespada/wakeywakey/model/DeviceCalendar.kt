package com.sierraespada.wakeywakey.model

/** Un calendario disponible en el dispositivo (Google, Outlook, Exchange…). */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int,      // ARGB
    val isVisible: Boolean = true,
)
