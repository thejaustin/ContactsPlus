package com.contactsplus.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contactsplus.app.models.SocialLink
import com.contactsplus.app.models.SocialPlatform
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialLinkDao {

    @Query("SELECT * FROM social_links WHERE contact_lookup_key = :lookupKey ORDER BY created_at ASC")
    suspend fun getLinksForContact(lookupKey: String): List<SocialLink>

    @Query("SELECT * FROM social_links WHERE contact_lookup_key = :lookupKey ORDER BY created_at ASC")
    fun getLinksForContactFlow(lookupKey: String): Flow<List<SocialLink>>

    @Query("SELECT * FROM social_links WHERE id = :id")
    suspend fun getLinkById(id: Long): SocialLink?

    @Query("SELECT * FROM social_links WHERE platform = :platform")
    suspend fun getLinksByPlatform(platform: SocialPlatform): List<SocialLink>

    @Query("SELECT * FROM social_links")
    suspend fun getAllLinks(): List<SocialLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: SocialLink): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<SocialLink>)

    @Update
    suspend fun update(link: SocialLink)

    @Delete
    suspend fun delete(link: SocialLink)

    @Query("DELETE FROM social_links WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM social_links WHERE contact_lookup_key = :lookupKey")
    suspend fun deleteAllForContact(lookupKey: String)

    @Query("SELECT COUNT(*) FROM social_links WHERE contact_lookup_key = :lookupKey")
    suspend fun getCountForContact(lookupKey: String): Int
}
