package com.contactsplus.app.helpers

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.contactsplus.app.models.SocialPlatform
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.fossify.commons.helpers.ContactsHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

@Parcelize
data class PotentialMatch(
    val name: String,
    val platform: SocialPlatform,
    val username: String,
    var suggestedContactLookupKey: String? = null,
    var suggestedContactName: String? = null,
    var suggestedContactPhotoUri: String? = null,
    var birthday: String? = null // Format: YYYY-MM-DD or --MM-DD
) : Parcelable

class SocialMediaImporter(private val context: Context) {

    fun parseBackup(uri: Uri, onResult: (List<PotentialMatch>) -> Unit) {
        val matches = ArrayList<PotentialMatch>()
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return onResult(emptyList())

            if (uri.toString().endsWith(".zip") || contentResolver.getType(uri) == "application/zip") {
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.getNextEntry()
                while (entry != null) {
                    // Support for split files like followers_1.json, friends_2.json
                    if (entry.name.endsWith(".json")) {
                        val content = zipInputStream.bufferedReader().readText()
                        parseJsonContent(content, entry.name, matches)
                    }
                    entry = zipInputStream.getNextEntry()
                }
                zipInputStream.close()
            } else {
                val content = inputStream.bufferedReader().readText()
                parseJsonContent(content, uri.lastPathSegment ?: "", matches)
                inputStream.close()
            }

            // After parsing, suggest contacts from device
            ContactsHelper(context).getContacts { contacts ->
                matches.forEach { match ->
                    val contact = contacts.find { 
                        it.getNameToDisplay().contains(match.name, ignoreCase = true) || 
                        match.name.contains(it.getNameToDisplay(), ignoreCase = true) ||
                        (match.username.isNotEmpty() && it.getNameToDisplay().contains(match.username, ignoreCase = true))
                    }
                    if (contact != null) {
                        match.suggestedContactLookupKey = contact.id.toString()
                        match.suggestedContactName = contact.getNameToDisplay()
                        match.suggestedContactPhotoUri = contact.thumbnailUri
                    }
                }
                onResult(matches)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onResult(emptyList())
        }
    }

    private fun parseJsonContent(content: String, fileName: String, matches: MutableList<PotentialMatch>) {
        try {
            val json = JSONObject(content)

            // Snapchat: friends.json (Latest format uses "Friends" with Capital F)
            if (json.has("Friends") || json.has("friends")) {
                val key = if (json.has("Friends")) "Friends" else "friends"
                val friends = json.getJSONArray(key)
                for (i in 0 until friends.length()) {
                    val friend = friends.getJSONObject(i)
                    val displayName = friend.optString("Display Name", friend.optString("display_name", ""))
                    val username = friend.optString("Username", friend.optString("username", ""))
                    if (username.isNotEmpty()) {
                        matches.add(PotentialMatch(if (displayName.isEmpty()) username else displayName, SocialPlatform.SNAPCHAT, username))
                    }
                }
            }

            // Instagram: following.json, followers.json, relationships_following.json
            // Latest format has a top level array or a wrapper
            val instaKeys = listOf("relationships_following", "relationships_followers", "relationships_following_requests_sent")
            instaKeys.forEach { key ->
                if (json.has(key)) {
                    val list = json.getJSONArray(key)
                    for (i in 0 until list.length()) {
                        val item = list.getJSONObject(i)
                        val stringListData = item.optJSONArray("string_list_data")
                        if (stringListData != null && stringListData.length() > 0) {
                            val data = stringListData.getJSONObject(0)
                            val value = data.getString("value")
                            matches.add(PotentialMatch(value, SocialPlatform.INSTAGRAM, value))
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
                        // Check for birthday in custom/extended exports
                        val birthday = friend.optString("birthday", null) 
                        matches.add(PotentialMatch(name, SocialPlatform.MESSENGER, contactInfo, birthday = birthday))
                    }
                }
            }
            
            if (json.has("address_book")) {
                val addressBook = json.getJSONObject("address_book")
                if (addressBook.has("address_book")) {
                    val entries = addressBook.getJSONArray("address_book")
                    for (i in 0 until entries.length()) {
                        val entry = entries.getJSONObject(i)
                        val name = entry.optString("name", "")
                        val details = entry.optJSONArray("details")
                        var contactInfo = ""
                        if (details != null && details.length() > 0) {
                            contactInfo = details.getJSONObject(0).optString("contact_point", "")
                        }
                        if (name.isNotEmpty()) {
                            matches.add(PotentialMatch(name, SocialPlatform.MESSENGER, contactInfo))
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Might be a JSONArray (common in some FB/Insta sub-files)
            try {
                val array = org.json.JSONArray(content)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    // FB direct array
                    if (obj.has("name") && (obj.has("timestamp") || obj.has("contact_info"))) {
                        val name = obj.getString("name")
                        val contactInfo = obj.optString("contact_info", "")
                        val birthday = obj.optString("birthday", null)
                        matches.add(PotentialMatch(name, SocialPlatform.MESSENGER, contactInfo, birthday = birthday))
                    }
                    // Insta direct array (string_list_data)
                    val stringListData = obj.optJSONArray("string_list_data")
                    if (stringListData != null && stringListData.length() > 0) {
                        val value = stringListData.getJSONObject(0).getString("value")
                        matches.add(PotentialMatch(value, SocialPlatform.INSTAGRAM, value))
                    }
                }
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    private fun java.io.InputStream.bufferedReader() = BufferedReader(InputStreamReader(this))
}
