@file:Suppress("TooManyFunctions", "LongMethod", "ComplexMethod", "MagicNumber", "unused")

package com.contactsplus.app.utils

import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.Breadcrumb

/**
 * Simple Sentry helper for basic error tracking.
 */
object SentryHelper {

    /**
     * Track an error/exception with optional context.
     */
    fun trackError(throwable: Throwable, context: String = "General") {
        Sentry.captureException(throwable) { scope ->
            scope.level = SentryLevel.ERROR
            scope.setTag("context", context)
            scope.addBreadcrumb(Breadcrumb().apply {
                category = "error"
                message = "Error in $context"
                level = SentryLevel.ERROR
            })
        }
    }

    /**
     * Track UI glitches like layout issues, missing views, or rendering problems.
     */
    fun trackUiGlitch(glitchType: String, details: String) {
        Sentry.captureMessage("UI Glitch: $glitchType") { scope ->
            scope.level = SentryLevel.WARNING
            scope.setTag("type", glitchType)
            scope.setTag("details", details)
            scope.addBreadcrumb(Breadcrumb().apply {
                category = "ui"
                message = "UI Glitch: $glitchType - $details"
                level = SentryLevel.WARNING
            })
        }
    }

    /**
     * Track rendering issues on specific screens.
     */
    fun trackRenderingIssue(screen: String, issue: String) {
        Sentry.captureMessage("Rendering: $issue") { scope ->
            scope.level = SentryLevel.ERROR
            scope.setTag("screen", screen)
            scope.setTag("issue", issue)
            scope.addBreadcrumb(Breadcrumb().apply {
                category = "rendering"
                message = "Rendering issue on $screen: $issue"
                level = SentryLevel.ERROR
            })
        }
    }

    /**
     * Track navigation between screens.
     */
    fun trackNavigation(from: String?, to: String) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "navigation"
            message = "$from -> $to"
            level = SentryLevel.INFO
            setData("from", from ?: "app_start")
            setData("to", to)
        })
    }

    /**
     * Track user actions (button clicks, dialog interactions, etc.).
     */
    fun trackUserAction(actionName: String, details: Map<String, String> = emptyMap()) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "user_action"
            message = actionName
            level = SentryLevel.INFO
            details.forEach { (key, value) ->
                setData(key, value)
            }
        })
    }

    /**
     * Track performance metrics (e.g., load times).
     */
    fun trackPerformance(operation: String, durationMs: Long) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "performance"
            message = "$operation took ${durationMs}ms"
            level = SentryLevel.INFO
            setData("operation", operation)
            setData("duration_ms", durationMs.toString())
        })

        // Only report slow operations to Sentry
        if (durationMs > 1000) {
            Sentry.captureMessage("Slow operation: $operation") { scope ->
                scope.level = SentryLevel.WARNING
                scope.setTag("operation", operation)
                scope.setTag("duration_ms", durationMs.toString())
            }
        }
    }

    /**
     * Track network operations (imports, exports, sync).
     */
    fun trackNetworkOperation(operation: String, success: Boolean, errorMessage: String? = null) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "network"
            message = "$operation: ${if (success) "success" : "failed"}"
            level = if (success) SentryLevel.INFO else SentryLevel.WARNING
            setData("operation", operation)
            setData("success", success.toString())
            if (errorMessage != null) {
                setData("error", errorMessage)
            }
        })

        if (!success && errorMessage != null) {
            Sentry.captureMessage("Network operation failed: $operation") { scope ->
                scope.level = SentryLevel.WARNING
                scope.setTag("operation", operation)
                scope.addBreadcrumb(Breadcrumb().apply {
                    message = errorMessage
                    level = SentryLevel.WARNING
                })
            }
        }
    }
}
