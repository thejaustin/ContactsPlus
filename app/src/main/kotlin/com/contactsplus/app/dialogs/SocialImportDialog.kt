package com.contactsplus.app.dialogs

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contactsplus.app.R
import com.contactsplus.app.activities.SimpleActivity
import com.contactsplus.app.database.SocialLinksDatabase
import com.contactsplus.app.helpers.PotentialMatch
import com.contactsplus.app.helpers.SocialMediaImporter
import com.contactsplus.app.models.SocialLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ContactsHelper
import com.contactsplus.app.databinding.DialogSocialImportBinding
import com.contactsplus.app.databinding.ItemSocialImportBinding

class SocialImportDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var binding: DialogSocialImportBinding
    private var dialog: AlertDialog? = null
    private val PICK_JSON_FILE = 1002

    init {
        binding = DialogSocialImportBinding.inflate(LayoutInflater.from(activity))
        
        binding.importSocialSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            activity.startActivityForResult(intent, PICK_JSON_FILE)
        }

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(binding.root, this)
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    importSelected()
                    dismiss()
                }
            }
    }

    private var potentialMatches = ArrayList<PotentialMatch>()
    private val selectedMatches = HashSet<PotentialMatch>()

    fun handleFileResult(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val importer = SocialMediaImporter(activity)
            val matches = importer.parseBackup(uri)
            
            ContactsHelper(activity).getContacts { contacts ->
                val finalMatches = ArrayList<PotentialMatch>()
                
                matches.forEach { match ->
                    val contact = contacts.find { it.getNameToDisplay().equals(match.name, ignoreCase = true) }
                    if (contact != null) {
                        finalMatches.add(match)
                    }
                }
                
                potentialMatches = finalMatches
                selectedMatches.addAll(finalMatches)

                activity.runOnUiThread {
                    if (potentialMatches.isEmpty()) {
                        activity.toast("No matching contacts found in backup.")
                    } else {
                        binding.importSocialList.visibility = android.view.View.VISIBLE
                        binding.importSocialList.layoutManager = LinearLayoutManager(activity)
                        binding.importSocialList.adapter = MatchesAdapter()
                        activity.toast("Found ${potentialMatches.size} matches.")
                    }
                }
            }
        }
    }

    private fun importSelected() {
        CoroutineScope(Dispatchers.IO).launch {
            ContactsHelper(activity).getContacts { contacts ->
                var count = 0
                selectedMatches.forEach { match ->
                    val contact = contacts.find { it.getNameToDisplay().equals(match.name, ignoreCase = true) }
                    if (contact != null) {
                        val link = SocialLink(
                            contactLookupKey = contact.id.toString(),
                            platform = match.platform,
                            username = match.username
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            SocialLinksDatabase.getInstance(activity).socialLinkDao().insert(link)
                        }
                        count++
                    }
                }
                activity.runOnUiThread {
                    activity.toast("Imported $count links.")
                    callback()
                }
            }
        }
    }

    inner class MatchesAdapter : RecyclerView.Adapter<MatchesAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemSocialImportBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSocialImportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val match = potentialMatches[position]
            holder.binding.socialImportName.text = match.name
            holder.binding.socialImportDetail.text = "${match.platform.displayName}: ${match.username}"
            holder.binding.socialImportCheckbox.isChecked = selectedMatches.contains(match)

            holder.binding.socialImportCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedMatches.add(match) else selectedMatches.remove(match)
            }
        }

        override fun getItemCount() = potentialMatches.size
    }
}
