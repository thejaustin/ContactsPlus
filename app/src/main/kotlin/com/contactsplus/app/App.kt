@file:Suppress("TooManyFunctions", "LongMethod", "ComplexMethod", "unused")

package com.contactsplus.app

import android.app.Activity
import android.os.Bundle
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.fossify.commons.FossifyApp

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Minimal Sentry initialization for testing
        Sentry.init { options ->
            options.dsn = "https://1d0e5d7cd0f38cf3bca2cf7fd76aa98c@o4510887187841024.ingest.us.sentry.io/4511058249318400"
            options.environment = BuildConfig.BUILD_TYPE
            options.tracesSampleRate = 0.5
        }

        setupCrashTracking()
    }

    private fun setupCrashTracking() {
        // Track activity lifecycle
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Sentry.addBreadcrumb(io.sentry.Breadcrumb().apply {
                    category = "lifecycle"
                    message = "Activity Created: ${activity.localClassName}"
                    level = SentryLevel.INFO
                })
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
