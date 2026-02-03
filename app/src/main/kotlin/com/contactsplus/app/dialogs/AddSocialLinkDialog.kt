package com.contactsplus.app.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.contactsplus.app.R
import com.contactsplus.app.databinding.DialogAddSocialLinkBinding
import com.contactsplus.app.models.SocialLink
import com.contactsplus.app.models.SocialPlatform
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value

class AddSocialLinkDialog(
    private val activity: Activity,
    private val contactLookupKey: String,
    private val existingLink: SocialLink? = null,
    private val callback: (SocialLink) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogAddSocialLinkBinding.inflate(activity.layoutInflater)

    init {
        setupPlatformSpinner()

        if (existingLink != null) {
            binding.socialLinkUsername.setText(existingLink.username)
            binding.socialLinkCustomLabel.setText(existingLink.customLabel ?: "")
            val position = SocialPlatform.entries.indexOf(existingLink.platform)
            binding.socialLinkPlatformSpinner.setSelection(position)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, if (existingLink == null) R.string.add_social_link else R.string.edit_social_link) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(binding.socialLinkUsername)

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val username = binding.socialLinkUsername.value.trim()
                        if (username.isEmpty()) {
                            activity.toast(R.string.enter_username)
                            return@setOnClickListener
                        }

                        val selectedPosition = binding.socialLinkPlatformSpinner.selectedItemPosition
                        val platform = SocialPlatform.entries[selectedPosition]
                        val customLabel = binding.socialLinkCustomLabel.value.trim().takeIf { it.isNotEmpty() }

                        val socialLink = SocialLink(
                            id = existingLink?.id ?: 0,
                            contactLookupKey = contactLookupKey,
                            platform = platform,
                            username = username,
                            customLabel = customLabel,
                            createdAt = existingLink?.createdAt ?: System.currentTimeMillis()
                        )

                        callback(socialLink)
                        alertDialog.dismiss()
                    }
                }
            }
    }

    private fun setupPlatformSpinner() {
        val platforms = SocialPlatform.entries.map { it.displayName }
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, platforms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.socialLinkPlatformSpinner.adapter = adapter
    }
}
