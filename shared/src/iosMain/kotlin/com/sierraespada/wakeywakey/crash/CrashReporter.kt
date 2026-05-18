package com.sierraespada.wakeywakey.crash

import platform.Foundation.NSLog

// Sentry iOS se inicializa desde Swift (AppDelegate) vía SPM: github.com/getsentry/sentry-cocoa.
// Este stub satisface el contrato expect/actual; los crashes llegarán a Sentry desde el lado Swift.
actual object CrashReporter {

    actual fun initialize(dsn: String, environment: String) {
        // No-op: Sentry.start(options:) se llama en AppDelegate.swift
    }

    actual fun captureException(throwable: Throwable, context: Map<String, Any>) {
        NSLog("[CrashReporter] ${throwable::class.simpleName}: ${throwable.message}")
    }

    actual fun setUser(id: String) {}

    actual fun clearUser() {}
}
