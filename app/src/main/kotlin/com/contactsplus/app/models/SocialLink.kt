package com.contactsplus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.text.DateFormat
import java.util.Date

@Entity(tableName = "social_links")
data class SocialLink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "contact_lookup_key")
    val contactLookupKey: String,

    @ColumnInfo(name = "platform")
    val platform: SocialPlatform,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "custom_label")
    val customLabel: String? = null,

    @ColumnInfo(name = "attribution")
    val attribution: String? = null,

    @ColumnInfo(name = "type")
    val type: Int = TYPE_PROFILE,

    @ColumnInfo(name = "connected_at")
    val connectedAt: Long? = null,

    @ColumnInfo(name = "is_close_friend")
    val isCloseFriend: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_PROFILE = 0
        const val TYPE_CHAT = 1
    }

    fun getDisplayLabel(): String = customLabel ?: platform.displayName

    fun getDeeplink(): String = if (type == TYPE_CHAT) {
        platform.buildChatDeeplink(username) ?: platform.buildDeeplink(username)
    } else {
        platform.buildDeeplink(username)
    }

    fun getWebUrl(): String = if (type == TYPE_CHAT) {
        platform.buildChatWebUrl(username) ?: platform.buildWebUrl(username)
    } else {
        platform.buildWebUrl(username)
    }

    fun getFormattedConnectionDate(): String? {
        if (connectedAt == null || connectedAt == 0L) return null
        return try {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(connectedAt))
        } catch (e: Exception) {
            null
        }
    }
}

class SocialPlatformConverter {
    @TypeConverter
    fun fromPlatform(platform: SocialPlatform): String = platform.name

    @TypeConverter
    fun toPlatform(value: String): SocialPlatform = SocialPlatform.valueOf(value)
}
