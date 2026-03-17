package com.contactsplus.app.utils

import android.util.Log
import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SpanStatus

/**
 * Helper object for easy Sentry tracking throughout the app.
 * Use this to track errors, UI glitches, performance issues, and user flows.
 *
 * Usage:
 *   SentryHelper.trackError(exception, "ContactLoad")
 *   SentryHelper.trackUiGlitch("RecyclerViewJank", "Frame drop during scroll")
 *   SentryHelper.startSpan("Database", "loadContacts") { ... }
 */
object SentryHelper {

    private const val TAG = "SentryHelper"

    /**
     * Track a caught exception that doesn't crash the app
     */
    fun trackError(
        throwable: Throwable,
        context: String = "General",
        data: Map<String, String> = emptyMap()
    ) {
        try {
            Sentry.captureException(throwable) { scope ->
                scope.setLevel(SentryLevel.ERROR)
                scope.setTag("error_context", context)
                scope.setTag("error_type", throwable.javaClass.simpleName)
                data.forEach { (key, value) ->
                    scope.setTag(key, value)
                }
            }
            Log.w(TAG, "Error tracked: $context - ${throwable.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track error", e)
        }
    }

    /**
     * Track a fatal error that would normally crash the app
     */
    fun trackFatalError(
        throwable: Throwable,
        context: String = "Fatal"
    ) {
        try {
            Sentry.captureException(throwable) { scope ->
                scope.setLevel(SentryLevel.FATAL)
                scope.setTag("error_context", context)
                scope.setTag("is_fatal", "true")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track fatal error", e)
        }
    }

    /**
     * Track UI glitches, visual issues, layout problems
     */
    fun trackUiGlitch(
        glitchType: String,
        details: String,
        severity: SentryLevel = SentryLevel.WARNING
    ) {
        try {
            Sentry.captureMessage("UI Glitch: $glitchType") { scope ->
                scope.setLevel(severity)
                scope.setTag("glitch_type", glitchType)
                scope.setTag("glitch_details", details)
                scope.setTag("category", "ui")
            }
            Log.w(TAG, "UI Glitch tracked: $glitchType - $details")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track UI glitch", e)
        }
    }

    /**
     * Track layout issues (missing views, wrong dimensions, etc.)
     */
    fun trackLayoutIssue(
        viewName: String,
        issue: String,
        expectedValue: String? = null,
        actualValue: String? = null
    ) {
        trackUiGlitch("LayoutIssue", "$viewName: $issue", SentryLevel.WARNING).also {
            Sentry.configureScope { scope ->
                scope.setContext("layout_debug", mapOf(
                    "view" to viewName,
                    "issue" to issue,
                    "expected" to (expectedValue ?: "N/A"),
                    "actual" to (actualValue ?: "N/A")
                ))
            }
        }
    }

    /**
     * Track rendering issues (blank screens, white screens, etc.)
     */
    fun trackRenderingIssue(
        screenName: String,
        issueType: String,
        duration: Long? = null
    ) {
        Sentry.captureMessage("Rendering Issue: $screenName") { scope ->
            scope.setLevel(SentryLevel.ERROR)
            scope.setTag("screen", screenName)
            scope.setTag("issue_type", issueType)
            scope.setTag("category", "rendering")
            if (duration != null) {
                scope.setData("load_duration_ms", duration)
            }
        }
    }

    /**
     * Track performance issues (slow operations, jank, etc.)
     */
    fun trackPerformanceIssue(
        operation: String,
        durationMs: Long,
        thresholdMs: Long = 1000
    ) {
        if (durationMs > thresholdMs) {
            Sentry.captureMessage("Slow Operation: $operation") { scope ->
                scope.setLevel(SentryLevel.WARNING)
                scope.setTag("operation", operation)
                scope.setData("duration_ms", durationMs)
                scope.setData("threshold_ms", thresholdMs)
                scope.setData("slowdown_factor", (durationMs.toDouble() / thresholdMs).toString())
            }
            Log.w(TAG, "Performance issue: $operation took ${durationMs}ms (threshold: ${thresholdMs}ms)")
        }
    }

    /**
     * Track network errors
     */
    fun trackNetworkError(
        url: String,
        method: String,
        error: String,
        statusCode: Int? = null
    ) {
        Sentry.captureMessage("Network Error: $url") { scope ->
            scope.setLevel(SentryLevel.ERROR)
            scope.setTag("url", url)
            scope.setTag("method", method)
            scope.setTag("error", error)
            scope.setTag("category", "network")
            if (statusCode != null) {
                scope.setTag("status_code", statusCode.toString())
            }
        }
    }

    /**
     * Track database errors
     */
    fun trackDatabaseError(
        operation: String,
        table: String? = null,
        error: Throwable
    ) {
        Sentry.captureException(error) { scope ->
            scope.setLevel(SentryLevel.ERROR)
            scope.setTag("db_operation", operation)
            scope.setTag("db_table", table ?: "unknown")
            scope.setTag("category", "database")
        }
    }

    /**
     * Track user flow for debugging
     */
    fun trackUserFlow(
        flowName: String,
        step: String,
        data: Map<String, String> = emptyMap()
    ) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "user_flow"
            message = "$flowName: $step"
            level = SentryLevel.INFO
            setData("flow", flowName)
            setData("step", step)
            data.forEach { (key, value) ->
                setData(key, value)
            }
        })
    }

    /**
     * Track screen navigation
     */
    fun trackNavigation(
        from: String?,
        to: String,
        action: String = "navigate"
    ) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "navigation"
            message = "Navigation: $from -> $to ($action)"
            level = SentryLevel.INFO
            setData("from", from ?: "external")
            setData("to", to)
            setData("action", action)
        })
    }

    /**
     * Track contact-related operations
     */
    fun trackContactOperation(
        operation: String,
        contactId: String? = null,
        success: Boolean = true,
        error: Throwable? = null
    ) {
        if (error != null || !success) {
            Sentry.captureMessage("Contact Operation Failed: $operation") { scope ->
                scope.setLevel(SentryLevel.ERROR)
                scope.setTag("operation", operation)
                scope.setTag("success", success.toString())
                scope.setTag("category", "contact")
                if (contactId != null) {
                    scope.setTag("contact_id", contactId)
                }
            }
        } else {
            Sentry.addBreadcrumb(Breadcrumb().apply {
                category = "contact"
                message = "Contact operation: $operation"
                level = SentryLevel.INFO
                setData("operation", operation)
                setData("success", success.toString())
            })
        }
    }

    /**
     * Track permission issues
     */
    fun trackPermissionIssue(
        permission: String,
        granted: Boolean,
        rationale: Boolean = false
    ) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            category = "permission"
            message = "Permission: $permission - granted: $granted"
            level = if (granted) SentryLevel.INFO else SentryLevel.WARNING
            setData("permission", permission)
            setData("granted", granted.toString())
            setData("rationale_shown", rationale.toString())
        })
    }

    /**
     * Start a performance span for tracing
     */
    inline fun <T> startSpan(
        operation: String,
        description: String,
        block: () -> T
    ): T {
        val transaction = Sentry.startTransaction(operation, description)
        return try {
            val result = block()
            transaction.status = SpanStatus.OK
            result
        } catch (e: Exception) {
            transaction.status = SpanStatus.INTERNAL_ERROR
            transaction.throwable = e
            throw e
        } finally {
            transaction.finish()
        }
    }

    /**
     * Start a performance span with result
     */
    inline fun <T> startSpanWithData(
        operation: String,
        description: String,
        block: (MutableMap<String, String>) -> T
    ): T {
        val transaction = Sentry.startTransaction(operation, description)
        val data = mutableMapOf<String, String>()
        return try {
            val result = block(data)
            transaction.status = SpanStatus.OK
            data.forEach { (key, value) ->
                transaction.setData(key, value)
            }
            result
        } catch (e: Exception) {
            transaction.status = SpanStatus.INTERNAL_ERROR
            transaction.throwable = e
            throw e
        } finally {
            transaction.finish()
        }
    }

    /**
     * Add a custom breadcrumb
     */
    fun addBreadcrumb(
        message: String,
        category: String = "custom",
        level: SentryLevel = SentryLevel.INFO,
        data: Map<String, String> = emptyMap()
    ) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.message = message
            this.category = category
            this.level = level
            data.forEach { (key, value) ->
                setData(key, value)
            }
        })
    }

    /**
     * Clear all breadcrumbs (use sparingly)
     */
    fun clearBreadcrumbs() {
        Sentry.clearBreadcrumbs()
    }

    /**
     * Set a user context (anonymous ID, not PII)
     */
    fun setUserContext(userId: String?) {
        Sentry.setUser(userId)
    }

    /**
     * Set a tag for all subsequent events
     */
    fun setTag(key: String, value: String) {
        Sentry.configureScope { scope ->
            scope.setTag(key, value)
        }
    }

    /**
     * Remove a tag
     */
    fun removeTag(key: String) {
        Sentry.configureScope { scope ->
            scope.removeTag(key)
        }
    }
}
