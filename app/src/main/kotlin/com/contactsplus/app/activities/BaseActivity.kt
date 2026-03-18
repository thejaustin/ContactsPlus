package com.contactsplus.app.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.contactsplus.app.extensions.*
import com.contactsplus.app.helpers.*
import com.contactsplus.app.models.FAQItem
import com.contactsplus.app.utils.SentryHelper

/**
 * Base activity class for Contacts+ app.
 *
 * This is a fork of Fossify Commons' BaseSimpleActivity with the "fake version" warning removed.
 * The original package name validation check has been intentionally removed as this is a
 * legitimate fork of the original Simple Mobile Tools / Fossify project.
 */
abstract class BaseActivity : AppCompatActivity() {
    var isAskingPermissions = false
    var useDynamicTheme = true
    private lateinit var backCallback: OnBackPressedCallback

    // Abstract methods - must be implemented by subclasses
    abstract fun getAppIconIDs(): ArrayList<Int>
    abstract fun getAppLauncherName(): String
    abstract fun getRepositoryName(): String?

    // Open methods - can be overridden
    protected open fun onBackPressedCompat(): Boolean = false

    protected fun performDefaultBack() {
        backCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        backCallback.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            if (useDynamicTheme) {
                setTheme(getThemeId(showTransparentTop = true))
            }
            installFontInflaterFactory()
            super.onCreate(savedInstanceState)
            WindowCompat.enableEdgeToEdge(window)
            registerBackPressedCallback()
            // NOTE: Package name validation check has been intentionally removed.
            // This is a legitimate fork of the Fossify/Simple Mobile Tools project.
            // The "fake version" warning will NEVER appear.
            SentryHelper.trackNavigation(null, localClassName)
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onCreate")
            throw e
        }
    }

    override fun onResume() {
        try {
            super.onResume()
            if (useDynamicTheme) {
                setTheme(getThemeId(showTransparentTop = true))
                updateBackgroundColor(getProperBackgroundColor())
            }
            updateRecentsAppIcon()
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onResume")
        }
    }

    override fun onPause() {
        try {
            super.onPause()
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onPause")
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onDestroy")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        try {
            super.onConfigurationChanged(newConfig)
            ViewCompat.requestApplyInsets(findViewById(android.R.id.content))
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onConfigurationChanged")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                android.R.id.home -> {
                    hideKeyboard()
                    finish()
                }
                else -> return super.onOptionsItemSelected(item)
            }
            return true
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onOptionsItemSelected")
            return false
        }
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            if (newBase.baseConfig.useEnglish && !isTiramisuPlus()) {
                super.attachBaseContext(MyContextWrapper(newBase).wrap(newBase, "en"))
            } else {
                super.attachBaseContext(newBase)
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.attachBaseContext")
            super.attachBaseContext(newBase)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            isAskingPermissions = false
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onRequestPermissionsResult")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.onActivityResult")
        }
    }

    protected fun registerBackPressedCallback() {
        try {
            backCallback = onBackPressedDispatcher.addCallback(this) {
                if (onBackPressedCompat()) return@addCallback
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.registerBackPressedCallback")
        }
    }

    protected fun setBackHandlingEnabled(enabled: Boolean) {
        backCallback.isEnabled = enabled
    }

    private fun installFontInflaterFactory() {
        try {
            val inflater = layoutInflater
            if (inflater.factory2 != null) return
            val appCompatDelegate = delegate
            LayoutInflaterCompat.setFactory2(inflater, object : LayoutInflater.Factory2 {
                override fun onCreateView(
                    parent: View?,
                    name: String,
                    context: Context,
                    attrs: AttributeSet
                ): View? {
                    val view = appCompatDelegate.createView(parent, name, context, attrs)
                    val textView = view as? TextView ?: return view
                    applyFontToTextView(textView)
                    return view
                }

                override fun onCreateView(
                    name: String,
                    context: Context,
                    attrs: AttributeSet
                ): View? {
                    return onCreateView(null, name, context, attrs)
                }
            })
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.installFontInflaterFactory")
        }
    }

    fun updateBackgroundColor(color: Int = baseConfig.backgroundColor) {
        try {
            window.decorView.setBackgroundColor(color)
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.updateBackgroundColor")
        }
    }

    private fun updateRecentsAppIcon() {
        try {
            if (baseConfig.isUsingModifiedAppIcon) {
                val appIconIDs = getAppIconIDs()
                val currentAppIconColorIndex = getCurrentAppIconColorIndex()
                if (appIconIDs.size - 1 < currentAppIconColorIndex) {
                    return
                }
                val recentsIcon = BitmapFactory.decodeResource(resources, appIconIDs[currentAppIconColorIndex])
                val title = getAppLauncherName()
                val color = baseConfig.primaryColor
                val description = ActivityManager.TaskDescription(title, recentsIcon, color)
                setTaskDescription(description)
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.updateRecentsAppIcon")
        }
    }

    private fun getCurrentAppIconColorIndex(): Int {
        val appIconColor = baseConfig.appIconColor
        getAppIconColors().forEachIndexed { index, color ->
            if (color == appIconColor) {
                return index
            }
        }
        return 0
    }

    fun updateMenuItemColors(
        menu: Menu?,
        baseColor: Int = getProperStatusBarColor(),
        forceWhiteIcons: Boolean = false
    ) {
        try {
            if (menu == null) {
                return
            }
            var color = baseColor.getContrastColor()
            if (forceWhiteIcons) {
                color = Color.WHITE
            }
            for (i in 0 until menu.size) {
                try {
                    menu[i].icon?.setTint(color)
                } catch (ignored: Exception) {
                }
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.updateMenuItemColors")
        }
    }

    /**
     * Start the About activity - NO fake version check.
     * This is our own implementation without the warning.
     */
    fun startAboutActivity(
        appNameId: Int,
        licenseMask: Long,
        versionName: String,
        faqItems: ArrayList<FAQItem>,
        showFAQBeforeMail: Boolean
    ) {
        try {
            hideKeyboard()
            Intent(applicationContext, AboutActivity::class.java).apply {
                putExtra(APP_ICON_IDS, getAppIconIDs())
                putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
                putExtra(APP_NAME, getString(appNameId))
                putExtra(APP_REPOSITORY_NAME, getRepositoryName())
                putExtra(APP_LICENSES, licenseMask)
                putExtra(APP_VERSION_NAME, versionName)
                putExtra(APP_PACKAGE_NAME, baseConfig.appId)
                putExtra(APP_FAQ, faqItems)
                putExtra(SHOW_FAQ_BEFORE_MAIL, showFAQBeforeMail)
                startActivity(this)
                SentryHelper.trackNavigation(localClassName, "AboutActivity")
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.startAboutActivity")
        }
    }

    /**
     * Start the Customization activity - NO fake version check.
     * This is our own implementation without the warning.
     */
    fun startCustomizationActivity() {
        try {
            // No package name check - this is a legitimate fork
            Intent(applicationContext, CustomizationActivity::class.java).apply {
                putExtra(APP_ICON_IDS, getAppIconIDs())
                putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
                startActivity(this)
                SentryHelper.trackNavigation(localClassName, "CustomizationActivity")
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.startCustomizationActivity")
        }
    }

    /**
     * Handle permission request.
     */
    fun handlePermission(
        permissionId: Int,
        callback: (granted: Boolean) -> Unit
    ) {
        try {
            if (hasPermission(permissionId)) {
                callback(true)
            } else {
                isAskingPermissions = true
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(getPermissionString(permissionId)),
                    GENERIC_PERM_HANDLER
                )
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.handlePermission")
        }
    }

    /**
     * Handle notification permission.
     */
    fun handleNotificationPermission(callback: (granted: Boolean) -> Unit) {
        try {
            if (!isTiramisuPlus()) {
                callback(true)
            } else {
                handlePermission(PERMISSION_POST_NOTIFICATIONS) { granted ->
                    callback(granted)
                }
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.handleNotificationPermission")
        }
    }

    /**
     * SAF dialog handler - no package name check.
     */
    fun handleSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        try {
            hideKeyboard()
            callback(true)
            return false
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.handleSAFDialog")
            return false
        }
    }

    /**
     * SAF dialog handler for SDK 30+ - no package name check.
     */
    fun handleSAFDialogSdk30(
        path: String,
        showRationale: Boolean = true,
        callback: (success: Boolean) -> Unit
    ): Boolean {
        try {
            hideKeyboard()
            callback(true)
            return false
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.handleSAFDialogSdk30")
            return false
        }
    }

    /**
     * Check manage media or handle SAF dialog for SDK 30+.
     */
    fun checkManageMediaOrHandleSAFDialogSdk30(
        path: String,
        callback: (success: Boolean) -> Unit
    ): Boolean {
        try {
            hideKeyboard()
            if (canManageMedia()) {
                callback(true)
                false
            } else {
                handleSAFDialogSdk30(path = path, callback = callback)
            }
        } catch (e: Exception) {
            SentryHelper.trackError(e, "BaseActivity.checkManageMediaOrHandleSAFDialogSdk30")
            return false
        }
    }

    companion object {
        private const val GENERIC_PERM_HANDLER = 100
    }
}
