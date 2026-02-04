package com.contactsplus.app.dialogs

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contactsplus.app.R
import com.contactsplus.app.database.SocialLinksDatabase
import com.contactsplus.app.databinding.DialogDetectSocialLinksBinding
import com.contactsplus.app.databinding.ItemDetectedSocialLinkBinding
import com.contactsplus.app.helpers.SocialLinkDetector
import com.contactsplus.app.models.SocialLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.models.contacts.Contact

class DetectSocialLinksDialog(
    private val activity: Activity,
    private val contact: Contact,
    private val contactId: String,
    private val onLinksImported: () -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogDetectSocialLinksBinding.inflate(LayoutInflater.from(activity))
    private val database = SocialLinksDatabase.getInstance(activity)
    private var detectedLinks = mutableListOf<SocialLinkDetector.DetectedLink>()
    private var selectedLinks = mutableSetOf<Int>()
    private lateinit var adapter: DetectedLinksAdapter

    init {
        setupRecyclerView()
        detectLinks()

        binding.importSelectedButton.setOnClickListener {
            importSelectedLinks()
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.detect_social_links) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = DetectedLinksAdapter()
        binding.detectedLinksList.layoutManager = LinearLayoutManager(activity)
        binding.detectedLinksList.adapter = adapter
    }

    private fun detectLinks() {
        binding.detectProgress.beVisible()
        binding.detectedLinksList.beGone()
        binding.noLinksFound.beGone()
        binding.importSelectedButton.beGone()

        CoroutineScope(Dispatchers.IO).launch {
            // Get existing social links to avoid duplicates
            val existingLinks = database.socialLinkDao().getLinksForContact(contactId)
            val existingUsernames = existingLinks.map { "${it.platform.name}:${it.username.lowercase()}" }.toSet()

            // Detect new links
            val allDetected = SocialLinkDetector.detectFromContact(contact, contactId)

            // Filter out already existing links
            val newLinks = allDetected.filter { detected ->
                val key = "${detected.platform.name}:${detected.username.lowercase()}"
                !existingUsernames.contains(key)
            }.sortedByDescending { it.confidence }

            withContext(Dispatchers.Main) {
                binding.detectProgress.beGone()

                detectedLinks.clear()
                detectedLinks.addAll(newLinks)

                if (newLinks.isEmpty()) {
                    binding.noLinksFound.beVisible()
                    binding.detectedLinksList.beGone()
                    binding.importSelectedButton.beGone()
                } else {
                    binding.noLinksFound.beGone()
                    binding.detectedLinksList.beVisible()
                    binding.importSelectedButton.beVisible()

                    // Select all by default
                    selectedLinks.clear()
                    selectedLinks.addAll(newLinks.indices)

                    adapter.notifyDataSetChanged()
                    updateImportButtonText()
                }
            }
        }
    }

    private fun updateImportButtonText() {
        val count = selectedLinks.size
        binding.importSelectedButton.text = activity.getString(R.string.import_selected) + " ($count)"
        binding.importSelectedButton.isEnabled = count > 0
    }

    private fun importSelectedLinks() {
        if (selectedLinks.isEmpty()) return

        val linksToImport = selectedLinks.map { index ->
            val detected = detectedLinks[index]
            SocialLink(
                contactLookupKey = contactId,
                platform = detected.platform,
                username = detected.username,
                customLabel = null
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            linksToImport.forEach { link ->
                database.socialLinkDao().insert(link)
            }

            withContext(Dispatchers.Main) {
                activity.toast(activity.getString(R.string.social_links_imported, linksToImport.size))
                onLinksImported()
                dialog?.dismiss()
            }
        }
    }

    private fun getSourceText(source: String): String {
        return when (source) {
            "website" -> activity.getString(R.string.from_websites)
            "notes" -> activity.getString(R.string.from_notes)
            "im" -> activity.getString(R.string.from_messaging)
            else -> source
        }
    }

    inner class DetectedLinksAdapter : RecyclerView.Adapter<DetectedLinksAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemDetectedSocialLinkBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDetectedSocialLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val detected = detectedLinks[position]

            holder.binding.apply {
                detectedLinkIcon.setImageResource(detected.platform.iconRes)
                detectedLinkUsername.text = detected.username
                detectedLinkPlatform.text = detected.platform.displayName
                detectedLinkSource.text = getSourceText(detected.source)
                detectedLinkCheckbox.isChecked = selectedLinks.contains(position)

                root.setOnClickListener {
                    detectedLinkCheckbox.isChecked = !detectedLinkCheckbox.isChecked
                    toggleSelection(position, detectedLinkCheckbox.isChecked)
                }

                detectedLinkCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    toggleSelection(position, isChecked)
                }
            }
        }

        private fun toggleSelection(position: Int, isSelected: Boolean) {
            if (isSelected) {
                selectedLinks.add(position)
            } else {
                selectedLinks.remove(position)
            }
            updateImportButtonText()
        }

        override fun getItemCount() = detectedLinks.size
    }
}
