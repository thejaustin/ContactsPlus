package com.contactsplus.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.performance.AppStartMetrics
import org.fossify.commons.FossifyApp
import java.io.PrintWriter
import java.io.StringWriter

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initSentry()
        setupCrashTracking()
    }

    private fun initSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = "https://1d0e5d7cd0f38cf3bca2cf7fd76aa98c@o4510887187841024.ingest.us.sentry.io/4511058249318400"
            options.environment = BuildConfig.BUILD_TYPE
            options.release = "contacts-plus@${BuildConfig.VERSION_NAME}"

            // Performance monitoring - increased sample rate for debugging
            options.tracesSampleRate = 0.5
            options.profilesSampleRate = 0.3
            options.isEnableAppStartProfiling = true
            
            // App start tracking
            options.isEnableAutoPerformanceTracing = true
            options.isEnablePerformanceV2 = true

            // ANR detection - more aggressive
            options.isAnrEnabled = true
            options.anrTimeoutIntervalMillis = 3000

            // Session tracking
            options.isEnableAutoSessionTracking = true
            options.sessionTrackingIntervalMillis = 30000L

            // Comprehensive breadcrumbs
            options.isEnableUserInteractionBreadcrumbs = true
            options.isEnableAppLifecycleBreadcrumbs = true
            options.isEnableSystemEventBreadcrumbs = true
            options.isEnableNetworkEventBreadcrumbs = true
            options.isEnableWebViewBreadcrumbs = true
            options.isEnableScreenshotBreadcrumbs = true

            // Memory & CPU monitoring
            options.isEnableMemoryMonitoring = true
            options.isEnableCpuMonitoring = true
            options.memoryMonitoringIntervalMillis = 5000L
            options.cpuMonitoringIntervalMillis = 5000L

            // Network monitoring
            options.isEnableNetworkBreadcrumbs = true
            options.isEnableNetworkEventMonitoring = true

            // Attach more context
            options.isAttachThreads = true
            options.isAttachStacktrace = true
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            
            // Capture more exceptions
            options.isReportHistoricalAnrs = true
            options.isEnableBeforeSend = true

            // Filter sensitive data but keep useful context
            options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                filterEvent(event, hint)
            }

            // Custom breadcrumb for app-specific events
            options.beforeBreadcrumb = SentryOptions.BeforeBreadcrumbCallback { breadcrumb, hint ->
                addDeviceContext(breadcrumb)
            }

            // Capture OOM
            options.isEnableOutOfMemoryTracking = true
            
            // Slow & frozen frames
            options.isEnableTimeToFullDisplayTracing = true
            options.setBeforeSendTransaction { transaction, hint ->
                trackFrameMetrics(transaction)
            }
        }
    }

    private fun filterEvent(event: SentryEvent, hint: Hint?): SentryEvent? {
        // Filter out noise but keep crashes
        val throwable = event.throwable
        
        // Skip known non-critical errors
        if (throwable?.message?.contains("Socket closed", ignoreCase = true) == true) {
            return null
        }
        
        // Add custom tags based on error type
        event.setTag("error_type", throwable?.javaClass?.simpleName ?: "unknown")
        event.setLevel(determineLevel(throwable))
        
        return event
    }

    private fun determineLevel(throwable: Throwable?): SentryLevel {
        return when (throwable) {
            is OutOfMemoryError -> SentryLevel.FATAL
            is StackOverflowError -> SentryLevel.FATAL
            is NullPointerException -> SentryLevel.ERROR
            is IllegalStateException -> SentryLevel.ERROR
            is IllegalArgumentException -> SentryLevel.WARNING
            else -> SentryLevel.ERROR
        }
    }

    private fun addDeviceContext(breadcrumb: Breadcrumb): Breadcrumb? {
        breadcrumb.setData("device_model", Build.MODEL)
        breadcrumb.setData("android_version", Build.VERSION.RELEASE)
        breadcrumb.setData("api_level", Build.VERSION.SDK_INT.toString())
        return breadcrumb
    }

    private fun trackFrameMetrics(transaction: io.sentry.ITransaction): io.sentry.ITransaction? {
        try {
            val metrics = AppStartMetrics.getInstance()
            transaction.setTag("app_start_type", metrics.appStartType.toString())
            transaction.setData("time_to_initial_display", metrics.timeToInitialDisplayMs)
            transaction.setData("time_to_full_display", metrics.timeToFullDisplayMs)
        } catch (e: Exception) {
            Log.w("Sentry", "Failed to track frame metrics", e)
        }
        return transaction
    }

    private fun setupCrashTracking() {
        // Global exception handler for uncaught exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            captureFatalError(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Track activity lifecycle for better context
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                addBreadcrumb("Activity Created", activity.localClassName)
                Sentry.configureScope { scope ->
                    scope.setTag("current_activity", activity.localClassName)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                addBreadcrumb("Activity Resumed", activity.localClassName)
            }

            override fun onActivityPaused(activity: Activity) {
                addBreadcrumb("Activity Paused", activity.localClassName)
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                addBreadcrumb("Activity Destroyed", activity.localClassName)
            }
        })
    }

    private fun captureFatalError(thread: Thread, throwable: Throwable) {
        try {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            
            Sentry.captureException(throwable) { scope ->
                scope.setLevel(SentryLevel.FATAL)
                scope.setTag("thread_name", thread.name)
                scope.setTag("thread_id", thread.id.toString())
                scope.setContext("stack_trace", writer.toString())
            }
            
            // Give Sentry time to send the event
            Thread.sleep(200)
        } catch (e: Exception) {
            Log.e("Sentry", "Failed to capture fatal error", e)
        }
    }

    private fun addBreadcrumb(category: String, message: String) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.category = category
            this.message = message
            this.level = SentryLevel.INFO
        })
    }

    /**
     * Track custom events throughout the app
     */
    fun trackEvent(name: String, data: Map<String, String> = emptyMap()) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.category = "custom"
            this.message = name
            this.level = SentryLevel.INFO
            data.forEach { (key, value) ->
                setData(key, value)
            }
        })
    }

    /**
     * Capture non-fatal errors with context
     */
    fun captureError(throwable: Throwable, context: String? = null) {
        Sentry.captureException(throwable) { scope ->
            scope.setLevel(SentryLevel.ERROR)
            if (context != null) {
                scope.setTag("error_context", context)
            }
        }
    }

    /**
     * Track UI glitches and visual issues
     */
    fun trackUiGlitch(glitchType: String, details: String) {
        Sentry.captureMessage("UI Glitch: $glitchType") { scope ->
            scope.setLevel(SentryLevel.WARNING)
            scope.setTag("glitch_type", glitchType)
            scope.setTag("glitch_details", details)
            scope.setContext("ui_state", getCurrentUiState())
        }
    }

    /**
     * Track slow operations
     */
    fun trackSlowOperation(operation: String, durationMs: Long, thresholdMs: Long = 1000) {
        if (durationMs > thresholdMs) {
            Sentry.captureMessage("Slow Operation: $operation") { scope ->
                scope.setLevel(SentryLevel.WARNING)
                scope.setTag("operation", operation)
                scope.setData("duration_ms", durationMs)
                scope.setData("threshold_ms", thresholdMs)
            }
        }
    }

    private fun getCurrentUiState(): Map<String, String> {
        return mapOf(
            "low_memory" to isLowRamDevice().toString(),
            "is_debug" to BuildConfig.DEBUG.toString(),
            "app_state" = "foreground"
        )
    }

    private fun isLowRamDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        return activityManager?.isLowRamDevice ?: false
    }

    companion object {
        lateinit var instance: App
            private set
    }
}