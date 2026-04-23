package com.contactsplus.app.helpers

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.contactsplus.app.models.SocialPlatform
import com.contactsplus.app.utils.SentryHelper
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.fossify.commons.helpers.ContactsHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

@Parcelize
data class SuggestedContact(
    val lookupKey: String,
    val name: String,
    val photoUri: String? = null
) : Parcelable

@Parcelize
data class PotentialMatch(
    val name: String,
    val platform: SocialPlatform,
    val username: String,
    var suggestedContacts: ArrayList<SuggestedContact> = arrayListOf(),
    var birthday: String? = null,
    var connectionTimestamp: Long? = null,
    var isCloseFriend: Boolean = false,
    var email: String? = null,
    var phoneNumber: String? = null
) : Parcelable

class SocialMediaImporter(private val context: Context) {

    fun parseBackup(uri: Uri, onResult: (List<PotentialMatch>) -> Unit) {
        val matches = ArrayList<PotentialMatch>()
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return onResult(emptyList())

            val fileName = uri.lastPathSegment ?: ""
            if (fileName.endsWith(".csv")) {
                parseCsvContent(inputStream.bufferedReader().readText(), matches)
            } else if (uri.toString().endsWith(".zip") || contentResolver.getType(uri) == "application/zip") {
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.getNextEntry()
                while (entry != null) {
                    if (entry.name.endsWith(".json")) {
                        val content = zipInputStream.bufferedReader().readText()
                        parseJsonContent(content, entry.name, matches)
                    } else if (entry.name.endsWith(".csv")) {
                        val content = zipInputStream.bufferedReader().readText()
                        parseCsvContent(content, matches)
                    }
                    entry = zipInputStream.getNextEntry()
                }
                zipInputStream.close()
            } else {
                val content = inputStream.bufferedReader().readText()
                parseJsonContent(content, fileName, matches)
                inputStream.close()
            }

            // After parsing, suggest contacts from device
            ContactsHelper(context).getContacts { contacts ->
                matches.forEach { match ->
                    val contact = contacts.find { 
                        it.getNameToDisplay().contains(match.name, ignoreCase = true) || 
                        match.name.contains(it.getNameToDisplay(), ignoreCase = true) ||
                        (match.username.isNotEmpty() && it.getNameToDisplay().contains(match.username, ignoreCase = true)) ||
                        (match.email?.isNotEmpty() == true && it.emails.any { e -> e.value.equals(match.email, ignoreCase = true) }) ||
                        (match.phoneNumber?.isNotEmpty() == true && it.phoneNumbers.any { p -> p.normalizedNumber == match.phoneNumber || p.value == match.phoneNumber })
                    }
                    if (contact != null) {
                        match.suggestedContacts.add(
                            SuggestedContact(
                                lookupKey = contact.id.toString(),
                                name = contact.getNameToDisplay(),
                                photoUri = contact.thumbnailUri
                            )
                        )
                    }
                }
                onResult(matches)
            }

        } catch (e: Exception) {
            SentryHelper.trackError(e, "SocialMediaImporter.parseBackup")
            onResult(emptyList())
        }
    }

    private fun parseJsonContent(content: String, fileName: String, matches: MutableList<PotentialMatch>) {
        try {
            val json = JSONObject(content)

            // Discord: relationships.json
            if (fileName.contains("relationships.json")) {
                // Discord can be a direct array, handled in catch block
            }

            // Snapchat: friends.json
            if (json.has("Friends") || json.has("friends")) {
                val key = if (json.has("Friends")) "Friends" else "friends"
                val friends = json.getJSONArray(key)
                for (i in 0 until friends.length()) {
                    val friend = friends.getJSONObject(i)
                    val displayName = friend.optString("Display Name", friend.optString("display_name", ""))
                    val username = friend.optString("Username", friend.optString("username", ""))
                    val tsStr = friend.optString("Creation Timestamp", friend.optString("timestamp", ""))
                    val timestamp = parseTimestamp(tsStr)
                    if (username.isNotEmpty()) {
                        matches.add(PotentialMatch(if (displayName.isEmpty()) username else displayName, SocialPlatform.SNAPCHAT, username, connectionTimestamp = timestamp))
                    }
                }
            }

            // Instagram: following.json, followers.json, close_friends.json
            val instaKeys = listOf("relationships_following", "relationships_followers", "relationships_close_friends")
            instaKeys.forEach { key ->
                if (json.has(key)) {
                    val list = json.getJSONArray(key)
                    val isCloseFriend = key == "relationships_close_friends"
                    for (i in 0 until list.length()) {
                        val item = list.getJSONObject(i)
                        val stringListData = item.optJSONArray("string_list_data")
                        if (stringListData != null && stringListData.length() > 0) {
                            val data = stringListData.getJSONObject(0)
                            val value = data.getString("value")
                            val timestamp = data.optLong("timestamp") * 1000 // Instagram uses seconds
                            matches.add(PotentialMatch(value, SocialPlatform.INSTAGRAM, value, connectionTimestamp = if (timestamp > 0) timestamp else null, isCloseFriend = isCloseFriend))
                        }
                    }
                }
            }

            // Facebook: friends.json or your_address_book.json
            if (json.has("friends")) {
                val friends = json.getJSONArray("friends")
                for (i in 0 until friends.length()) {
                    val friend = friends.getJSONObject(i)
                    if (friend.has("name")) {
                        val name = friend.getString("name")
                        val contactInfo = friend.optString("contact_info", "")
                        val timestamp = friend.optLong("timestamp") * 1000
                        matches.add(PotentialMatch(name, SocialPlatform.MESSENGER, contactInfo, connectionTimestamp = if (timestamp > 0) timestamp else null))
                    }
                }
            }

        } catch (e: Exception) {
            try {
                val array = org.json.JSONArray(content)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    // Discord
                    if (obj.has("user") && obj.has("type")) {
                        val user = obj.getJSONObject("user")
                        val username = user.getString("username")
                        val displayName = user.optString("display_name", username)
                        val since = obj.optString("since", "")
                        matches.add(PotentialMatch(displayName, SocialPlatform.DISCORD, username, connectionTimestamp = parseIsoTimestamp(since)))
                    }
                    // Meta standardized format (Direct array)
                    val stringListData = obj.optJSONArray("string_list_data")
                    if (stringListData != null && stringListData.length() > 0) {
                        val value = stringListData.getJSONObject(0).getString("value")
                        val timestamp = stringListData.getJSONObject(0).optLong("timestamp") * 1000
                        matches.add(PotentialMatch(value, SocialPlatform.INSTAGRAM, value, connectionTimestamp = if (timestamp > 0) timestamp else null))
                    }
                }
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    private fun parseCsvContent(content: String, matches: MutableList<PotentialMatch>) {
        val lines = content.lines()
        if (lines.isEmpty()) return
        
        // Simple LinkedIn Connections.csv parsing
        val header = lines[0]
        if (header.contains("First Name") && header.contains("Last Name")) {
            for (i in 1 until lines.size) {
                val line = lines[i]
                val parts = line.split(",") // Basic CSV split, could be improved with a real parser
                if (parts.size >= 3) {
                    val firstName = parts[0].replace("\"", "")
                    val lastName = parts[1].replace("\"", "")
                    val email = parts[2].replace("\"", "")
                    val url = if (parts.size > 3) parts[3].replace("\"", "") else ""
                    val username = url.substringAfter("linkedin.com/in/").substringBefore("?").substringBefore("/")
                    
                    if (firstName.isNotEmpty()) {
                        matches.add(PotentialMatch("$firstName $lastName", SocialPlatform.LINKEDIN, username, email = email))
                    }
                }
            }
        }
    }

    private fun parseTimestamp(ts: String): Long? {
        return try {
            // Try Unix seconds first
            if (ts.all { it.isDigit() }) ts.toLong() * 1000 else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseIsoTimestamp(ts: String): Long? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(ts).toEpochMilli()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun java.io.InputStream.bufferedReader() = BufferedReader(InputStreamReader(this))
}
