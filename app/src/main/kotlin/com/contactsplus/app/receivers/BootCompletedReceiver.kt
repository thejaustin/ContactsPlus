package com.contactsplus.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.commons.helpers.ensureBackgroundThread
import com.contactsplus.app.extensions.checkAndBackupContactsOnBoot

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                checkAndBackupContactsOnBoot()
            }
        }
    }
}
