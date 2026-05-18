@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sierraespada.wakeywakey.model

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

actual fun currentTimeMillis(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_REALTIME.toUInt(), ts.ptr)
    ts.tv_sec * 1000L + ts.tv_nsec / 1_000_000L
}
