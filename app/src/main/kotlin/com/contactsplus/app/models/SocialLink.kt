package com.contactsplus.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

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

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayLabel(): String = customLabel ?: platform.displayName

    fun getDeeplink(): String = platform.buildDeeplink(username)

    fun getWebUrl(): String = platform.buildWebUrl(username)
}

class SocialPlatformConverter {
    @TypeConverter
    fun fromPlatform(platform: SocialPlatform): String = platform.name

    @TypeConverter
    fun toPlatform(value: String): SocialPlatform = SocialPlatform.valueOf(value)
}
