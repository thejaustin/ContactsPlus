package com.contactsplus.app.dialogs

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.contactsplus.app.R
import com.contactsplus.app.activities.SimpleActivity
import com.contactsplus.app.activities.SocialMatchingActivity
import com.contactsplus.app.helpers.PotentialMatch
import com.contactsplus.app.helpers.SocialMediaImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import com.contactsplus.app.databinding.DialogSocialImportBinding

class SocialImportDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var binding: DialogSocialImportBinding
    private var dialog: AlertDialog? = null
    private val PICK_SOCIAL_FILE = 1002

    init {
        binding = DialogSocialImportBinding.inflate(LayoutInflater.from(activity))
        
        binding.importSocialSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                val extraMimetypes = arrayOf("application/json", "application/zip")
                putExtra(Intent.EXTRA_MIME_TYPES, extraMimetypes)
            }
            activity.startActivityForResult(intent, PICK_SOCIAL_FILE)
        }

        val builder = AlertDialog.Builder(activity)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)

        activity.setupDialogStuff(binding.root, builder, R.string.import_social_contacts) { alertDialog ->
            dialog = alertDialog
        }
    }

    fun handleFileResult(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val importer = SocialMediaImporter(activity)
            importer.parseBackup(uri) { matches ->
                activity.runOnUiThread {
                    if (matches.isEmpty()) {
                        activity.toast(R.string.no_matches_found)
                    } else {
                        val intent = Intent(activity, SocialMatchingActivity::class.java).apply {
                            putParcelableArrayListExtra("matches", ArrayList(matches))
                        }
                        activity.startActivity(intent)
                        dialog?.dismiss()
                        callback()
                    }
                }
            }
        }
    }
}
