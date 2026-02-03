package com.contactsplus.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.contactsplus.app.models.SocialLink
import com.contactsplus.app.models.SocialPlatformConverter

@Database(
    entities = [SocialLink::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(SocialPlatformConverter::class)
abstract class SocialLinksDatabase : RoomDatabase() {

    abstract fun socialLinkDao(): SocialLinkDao

    companion object {
        private const val DATABASE_NAME = "social_links.db"

        @Volatile
        private var instance: SocialLinksDatabase? = null

        fun getInstance(context: Context): SocialLinksDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SocialLinksDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SocialLinksDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
