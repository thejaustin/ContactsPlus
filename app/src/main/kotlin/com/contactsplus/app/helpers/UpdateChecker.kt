package com.contactsplus.app.helpers

import android.util.Log
import com.contactsplus.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/thejaustin/ContactsPlus/releases/latest"
    private const val APK_ASSET_NAME = "contacts-plus-2-foss-release.apk"

    // Matches patch number in tags like `v1.1.0-5`
    private val patchRegex = Regex("""v\d+\.\d+\.\d+-(\d+)""")

    private fun parsePatchNum(tag: String): Int? =
        patchRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull()

    private fun currentPatchNum(): Int? = parsePatchNum(BuildConfig.VERSION_NAME)

    /**
     * Fetches the latest stable release from GitHub.
     * Returns [UpdateInfo] if a newer patch is available, null otherwise.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentPatch = currentPatchNum()
            if (currentPatch == null) {
                Log.w(TAG, "Could not parse current patch from VERSION_NAME: ${BuildConfig.VERSION_NAME}")
                return@withContext null
            }

            val url = URL(LATEST_RELEASE_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "ContactsPlus/${BuildConfig.VERSION_NAME}")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.connect()

            if (conn.responseCode != 200) {
                Log.w(TAG, "HTTP ${conn.responseCode} fetching latest release")
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val release = JSONObject(body)
            val tag = release.getString("tag_name")
            val remotePatch = parsePatchNum(tag)
            if (remotePatch == null) {
                Log.w(TAG, "Could not parse patch number from tag: $tag")
                return@withContext null
            }

            if (remotePatch <= currentPatch) {
                Log.d(TAG, "Already up to date (current=$currentPatch, remote=$remotePatch)")
                return@withContext null
            }

            // Find the known APK asset
            val assets = release.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name") == APK_ASSET_NAME }
                ?.getString("browser_download_url")
                ?: run {
                    Log.w(TAG, "APK asset '$APK_ASSET_NAME' not found in release $tag")
                    return@withContext null
                }

            UpdateInfo(
                versionName = tag,
                downloadUrl = apkUrl,
                releaseNotes = release.optString("body", ""),
                publishedAt = release.optString("published_at", ""),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            null
        }
    }
}
