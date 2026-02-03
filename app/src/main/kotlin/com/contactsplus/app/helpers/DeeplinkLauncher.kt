package com.contactsplus.app.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.contactsplus.app.R
import com.contactsplus.app.models.SocialLink
import com.contactsplus.app.models.SocialPlatform

class DeeplinkLauncher(private val context: Context) {

    fun launch(link: SocialLink) {
        launch(link.platform, link.username)
    }

    fun launch(platform: SocialPlatform, username: String) {
        val deeplink = platform.buildDeeplink(username)
        val webUrl = platform.buildWebUrl(username)

        // For custom links, just try to open directly
        if (platform == SocialPlatform.CUSTOM) {
            launchUrl(username)
            return
        }

        // Try deeplink first if app is installed
        if (isAppInstalled(platform.packageName)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink)).apply {
                    setPackage(platform.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fall through to web URL
            }
        }

        // Fallback to web URL
        launchWebUrl(webUrl)
    }

    fun launchInstagram(username: String) = launch(SocialPlatform.INSTAGRAM, username)
    fun launchSnapchat(username: String) = launch(SocialPlatform.SNAPCHAT, username)
    fun launchWhatsApp(phoneNumber: String) = launch(SocialPlatform.WHATSAPP, phoneNumber)
    fun launchTelegram(username: String) = launch(SocialPlatform.TELEGRAM, username)
    fun launchDiscord(userId: String) = launch(SocialPlatform.DISCORD, userId)
    fun launchTikTok(username: String) = launch(SocialPlatform.TIKTOK, username)
    fun launchTwitter(username: String) = launch(SocialPlatform.TWITTER, username)
    fun launchLinkedIn(profileId: String) = launch(SocialPlatform.LINKEDIN, profileId)
    fun launchSignal(username: String) = launch(SocialPlatform.SIGNAL, username)
    fun launchMessenger(username: String) = launch(SocialPlatform.MESSENGER, username)
    fun launchThreads(username: String) = launch(SocialPlatform.THREADS, username)

    private fun isAppInstalled(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun launchUrl(url: String) {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.unknown_error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.unknown_error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @Volatile
        private var instance: DeeplinkLauncher? = null

        fun getInstance(context: Context): DeeplinkLauncher {
            return instance ?: synchronized(this) {
                instance ?: DeeplinkLauncher(context.applicationContext).also { instance = it }
            }
        }
    }
}
