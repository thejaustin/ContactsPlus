@file:Suppress("TooManyFunctions", "LongMethod", "ComplexMethod", "MagicNumber", "unused")

package com.contactsplus.app.utils

import io.sentry.Sentry
import io.sentry.SentryLevel

/**
 * Simple Sentry helper for basic error tracking.
 */
object SentryHelper {

    fun trackError(throwable: Throwable, context: String = "General") {
        Sentry.captureException(throwable) { scope ->
            scope.level = SentryLevel.ERROR
            scope.setTag("context", context)
        }
    }

    fun trackUiGlitch(glitchType: String, details: String) {
        Sentry.captureMessage("UI Glitch: $glitchType") { scope ->
            scope.level = SentryLevel.WARNING
            scope.setTag("type", glitchType)
            scope.setTag("details", details)
        }
    }

    fun trackRenderingIssue(screen: String, issue: String) {
        Sentry.captureMessage("Rendering: $issue") { scope ->
            scope.level = SentryLevel.ERROR
            scope.setTag("screen", screen)
        }
    }

    fun trackNavigation(from: String?, to: String) {
        Sentry.addBreadcrumb(io.sentry.Breadcrumb().apply {
            category = "navigation"
            message = "$from -> $to"
        })
    }
}
