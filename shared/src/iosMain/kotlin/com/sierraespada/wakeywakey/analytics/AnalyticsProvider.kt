package com.sierraespada.wakeywakey.analytics

import platform.Foundation.NSLog

// PostHog iOS se integra desde Swift (AppDelegate) vía SPM: github.com/PostHog/posthog-ios.
// Este stub satisface el contrato expect/actual. Slices futuros pueden reemplazarlo con
// un bridge Swift→KMP si se necesita trackear desde código compartido.
actual object AnalyticsProvider {

    private val noopAnalytics: Analytics = NoOpAnalytics

    actual fun initialize(apiKey: String, host: String) {
        // No-op: PostHog.setup() se llama en AppDelegate.swift
    }

    actual val instance: Analytics get() = noopAnalytics
}

private object NoOpAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any>) {
        NSLog("[Analytics] $event ${if (properties.isEmpty()) "" else properties.toString()}")
    }
    override fun identify(userId: String, traits: Map<String, Any>) {}
    override fun screen(screenName: String) {}
    override fun flush() {}
}
