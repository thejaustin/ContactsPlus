package com.contactsplus.app.helpers

import android.content.Context
import android.net.Uri
import com.contactsplus.app.models.SocialPlatform
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class PotentialMatch(
    val name: String,
    val platform: SocialPlatform,
    val username: String
)

class SocialMediaImporter(private val context: Context) {

    fun parseBackup(uri: Uri): List<PotentialMatch> {
        val matches = ArrayList<PotentialMatch>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()

            val json = JSONObject(content)

            // Try detecting Snapchat format
            if (json.has("friends")) {
                val friends = json.getJSONArray("friends")
                for (i in 0 until friends.length()) {
                    val friend = friends.getJSONObject(i)
                    // Snapchat
                    if (friend.has("display_name") && friend.has("username")) {
                        val name = friend.getString("display_name")
                        val username = friend.getString("username")
                        matches.add(PotentialMatch(name, SocialPlatform.SNAPCHAT, username))
                    }
                    // Facebook (simplified, only if we assume some field structure, 
                    // but standard friends.json only has 'name' and 'timestamp'.
                    // Without a username/ID/URL, we can't link it meaningfully 
                    // unless we assume the 'name' is the search query or we rely on user manually adding info.
                    // But if it has contact_info or similar (depending on export), we might find something.
                    // For now, we focus on Snapchat which gives usernames.
                }
            }
            
            // Try detecting generic or other formats if needed
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return matches
    }
}
