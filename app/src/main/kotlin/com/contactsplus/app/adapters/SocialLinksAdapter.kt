package com.contactsplus.app.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.contactsplus.app.databinding.ItemSocialLinkBinding
import com.contactsplus.app.models.SocialLink
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getProperTextColor

class SocialLinksAdapter(
    private val activity: Activity,
    private val socialLinks: MutableList<SocialLink>,
    private val onItemClick: (SocialLink) -> Unit,
    private val onItemLongClick: (SocialLink) -> Unit
) : RecyclerView.Adapter<SocialLinksAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSocialLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(socialLinks[position])
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(socialLinks[position])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSocialLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val socialLink = socialLinks[position]

        holder.binding.apply {
            socialLinkIcon.setImageResource(socialLink.platform.iconRes)
            socialLinkUsername.text = socialLink.username
            socialLinkPlatform.text = socialLink.getDisplayLabel()

            socialLinkUsername.setTextColor(activity.getProperTextColor())
            socialLinkPlatform.setTextColor(activity.getProperTextColor())
            socialLinkOpen.applyColorFilter(activity.getProperTextColor())
        }
    }

    override fun getItemCount() = socialLinks.size
}
