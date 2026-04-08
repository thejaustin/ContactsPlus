package com.contactsplus.app.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import org.fossify.commons.helpers.SHOW_TABS

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)

        const val AUTO_UPDATE_ENABLED = "auto_update_enabled"
        const val UPDATE_CHANNEL_DEV = "update_channel_dev"
        const val LAST_UPDATE_CHECK_TIME = "last_update_check_time"
    }

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactSources) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactSources)
            .apply()

    var showSocialAttribution: Boolean
        get() = prefs.getBoolean(SHOW_SOCIAL_ATTRIBUTION, false)
        set(showSocialAttribution) = prefs.edit().putBoolean(SHOW_SOCIAL_ATTRIBUTION, showSocialAttribution).apply()

    var autoUpdateEnabled: Boolean
        get() = prefs.getBoolean(AUTO_UPDATE_ENABLED, true)
        set(v) = prefs.edit().putBoolean(AUTO_UPDATE_ENABLED, v).apply()

    var updateChannelDev: Boolean
        get() = prefs.getBoolean(UPDATE_CHANNEL_DEV, false)
        set(v) = prefs.edit().putBoolean(UPDATE_CHANNEL_DEV, v).apply()

    var lastUpdateCheckTime: Long
        get() = prefs.getLong(LAST_UPDATE_CHECK_TIME, 0L)
        set(v) = prefs.edit().putLong(LAST_UPDATE_CHECK_TIME, v).apply()

}
