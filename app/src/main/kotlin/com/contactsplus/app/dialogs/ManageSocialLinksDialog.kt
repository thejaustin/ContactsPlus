package com.contactsplus.app.dialogs

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.contactsplus.app.R
import com.contactsplus.app.adapters.SocialLinksAdapter
import com.contactsplus.app.database.SocialLinksDatabase
import com.contactsplus.app.databinding.DialogManageSocialLinksBinding
import com.contactsplus.app.helpers.DeeplinkLauncher
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

class ManageSocialLinksDialog(
    private val activity: Activity,
    private val contactLookupKey: String,
    private val contact: Contact? = null,
    private val onLinksChanged: () -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogManageSocialLinksBinding.inflate(LayoutInflater.from(activity))
    private val database = SocialLinksDatabase.getInstance(activity)
    private var socialLinks = mutableListOf<SocialLink>()
    private lateinit var adapter: SocialLinksAdapter

    init {
        setupRecyclerView()
        loadSocialLinks()

        binding.addSocialLinkButton.setOnClickListener {
            showAddDialog()
        }

        binding.detectSocialLinksButton.setOnClickListener {
            showDetectDialog()
        }

        // Hide detect button if no contact is available
        if (contact == null) {
            binding.detectSocialLinksButton.beGone()
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.social_links) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = SocialLinksAdapter(
            activity = activity,
            socialLinks = socialLinks,
            onItemClick = { link ->
                DeeplinkLauncher.getInstance(activity).launch(link)
            },
            onItemLongClick = { link ->
                showEditDeleteDialog(link)
            }
        )

        binding.socialLinksRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.socialLinksRecyclerView.adapter = adapter
    }

    private fun loadSocialLinks() {
        CoroutineScope(Dispatchers.IO).launch {
            val links = database.socialLinkDao().getLinksForContact(contactLookupKey)

            withContext(Dispatchers.Main) {
                socialLinks.clear()
                socialLinks.addAll(links)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun updateEmptyState() {
        if (socialLinks.isEmpty()) {
            binding.emptyStateText.beVisible()
            binding.socialLinksRecyclerView.beGone()
        } else {
            binding.emptyStateText.beGone()
            binding.socialLinksRecyclerView.beVisible()
        }
    }

    private fun showAddDialog() {
        AddSocialLinkDialog(activity, contactLookupKey) { socialLink ->
            saveSocialLink(socialLink)
        }
    }

    private fun showDetectDialog() {
        contact?.let { c ->
            DetectSocialLinksDialog(activity, c, contactLookupKey) {
                loadSocialLinks()
                onLinksChanged()
            }
        }
    }

    private fun showEditDeleteDialog(link: SocialLink) {
        val items = arrayOf(
            activity.getString(org.fossify.commons.R.string.edit),
            activity.getString(org.fossify.commons.R.string.delete)
        )

        activity.getAlertDialogBuilder()
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showEditDialog(link)
                    1 -> deleteSocialLink(link)
                }
            }
            .show()
    }

    private fun showEditDialog(link: SocialLink) {
        AddSocialLinkDialog(activity, contactLookupKey, link) { updatedLink ->
            saveSocialLink(updatedLink)
        }
    }

    private fun saveSocialLink(link: SocialLink) {
        CoroutineScope(Dispatchers.IO).launch {
            database.socialLinkDao().insert(link)

            withContext(Dispatchers.Main) {
                activity.toast(R.string.social_link_added)
                loadSocialLinks()
                onLinksChanged()
            }
        }
    }

    private fun deleteSocialLink(link: SocialLink) {
        CoroutineScope(Dispatchers.IO).launch {
            database.socialLinkDao().delete(link)

            withContext(Dispatchers.Main) {
                activity.toast(R.string.social_link_removed)
                loadSocialLinks()
                onLinksChanged()
            }
        }
    }
}
