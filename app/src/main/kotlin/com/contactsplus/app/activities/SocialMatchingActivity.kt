package com.contactsplus.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.contactsplus.app.R
import com.contactsplus.app.database.SocialLinksDatabase
import com.contactsplus.app.databinding.ActivitySocialMatchingBinding
import com.contactsplus.app.databinding.ItemSocialMatchingCardBinding
import com.contactsplus.app.dialogs.SelectContactsDialog
import com.contactsplus.app.helpers.PotentialMatch
import com.contactsplus.app.models.SocialLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.PHOTO_UNCHANGED

class SocialMatchingActivity : SimpleActivity() {
    private lateinit var binding: ActivitySocialMatchingBinding
    private var matches = ArrayList<PotentialMatch>()
    private val confirmedMatches = ArrayList<PotentialMatch>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialMatchingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.socialMatchingToolbar.title = getString(R.string.social_matching)
        binding.socialMatchingToolbar.setNavigationOnClickListener { finish() }

        matches = intent.getParcelableArrayListExtra<PotentialMatch>("matches") ?: arrayListOf()
        if (matches.isEmpty()) {
            toast(R.string.no_matches_found)
            finish()
            return
        }

        setupViewPager()
        setupActions()
    }

    private fun setupViewPager() {
        binding.socialMatchingViewPager.adapter = MatchingAdapter()
        binding.socialMatchingViewPager.offscreenPageLimit = 3
        
        val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.spacing_lg)
        val offsetPx = resources.getDimensionPixelOffset(R.dimen.spacing_lg)
        binding.socialMatchingViewPager.setPageTransformer { page, position ->
            val offset = position * -(2 * offsetPx + pageMarginPx)
            if (position < -1) {
                page.translationX = -offset
            } else if (position <= 1) {
                val scaleFactor = 0.85f.coerceAtLeast(1 - Math.abs(position) * 0.15f)
                page.translationX = offset
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
                page.alpha = 0.5f.coerceAtLeast(1 - Math.abs(position))
            } else {
                page.alpha = 0f
            }
        }

        binding.socialMatchingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateProgress()
            }
        })
        
        updateProgress()
    }

    private fun updateProgress() {
        val current = binding.socialMatchingViewPager.currentItem + 1
        val total = matches.size
        binding.socialMatchingProgress.beVisible()
        binding.socialMatchingProgress.max = total
        binding.socialMatchingProgress.progress = current
    }

    private fun setupActions() {
        binding.socialMatchingSkip.setOnClickListener {
            moveToNext()
        }

        binding.socialMatchingConfirm.setOnClickListener {
            val currentPos = binding.socialMatchingViewPager.currentItem
            val match = matches[currentPos]
            if (match.suggestedContactLookupKey != null) {
                confirmedMatches.add(match)
                moveToNext()
            } else {
                showManualSearch()
            }
        }

        binding.socialMatchingSearch.setOnClickListener {
            showManualSearch()
        }
    }

    private fun moveToNext() {
        val current = binding.socialMatchingViewPager.currentItem
        if (current < matches.size - 1) {
            binding.socialMatchingViewPager.currentItem = current + 1
        } else {
            finishMatching()
        }
    }

    private fun showManualSearch() {
        val currentPos = binding.socialMatchingViewPager.currentItem
        val match = matches[currentPos]
        
        ContactsHelper(this).getContacts { contacts ->
            runOnUiThread {
                SelectContactsDialog(this, ArrayList(contacts), false, false) { selected, _ ->
                    val contact = selected.firstOrNull() ?: return@SelectContactsDialog
                    match.suggestedContactLookupKey = contact.id.toString()
                    match.suggestedContactName = contact.getNameToDisplay()
                    match.suggestedContactPhotoUri = contact.thumbnailUri
                    runOnUiThread {
                        binding.socialMatchingViewPager.adapter?.notifyItemChanged(currentPos)
                        toast(getString(R.string.social_link_added))
                    }
                }
            }
        }
    }

    private fun finishMatching() {
        if (confirmedMatches.isEmpty()) {
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = SocialLinksDatabase.getInstance(this@SocialMatchingActivity).socialLinkDao()
            confirmedMatches.forEach { match ->
                val attribution = if (match.isCloseFriend) {
                    "${match.platform.displayName} Close Friends"
                } else {
                    "${match.platform.displayName} Export"
                }

                val link = SocialLink(
                    contactLookupKey = match.suggestedContactLookupKey!!,
                    platform = match.platform,
                    username = match.username,
                    attribution = attribution,
                    connectedAt = match.connectionTimestamp,
                    isCloseFriend = match.isCloseFriend
                )
                dao.insert(link)

                // If birthday is available, update the contact
                if (match.birthday != null) {
                    val contact = ContactsHelper(this@SocialMatchingActivity).getContactWithId(match.suggestedContactLookupKey!!.toInt(), false)
                    if (contact != null) {
                        val birthdayEvent = org.fossify.commons.models.contacts.Event(match.birthday!!, android.provider.ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                        if (contact.events.none { it.type == android.provider.ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY }) {
                            contact.events.add(birthdayEvent)
                            ContactsHelper(this@SocialMatchingActivity).updateContact(contact, org.fossify.commons.helpers.PHOTO_UNCHANGED)
                        }
                    }
                }
            }
            
            runOnUiThread {
                toast(getString(R.string.confirmed_matches, confirmedMatches.size))
                finish()
            }
        }
    }

    inner class MatchingAdapter : RecyclerView.Adapter<MatchingAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemSocialMatchingCardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSocialMatchingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val match = matches[position]
            holder.binding.apply {
                cardPlatformIcon.setImageResource(match.platform.iconRes)
                cardSocialName.text = match.name
                
                var handleText = if (match.username == match.name) "" else "@${match.username}"
                if (match.birthday != null) {
                    if (handleText.isNotEmpty()) handleText += " • "
                    handleText += match.birthday
                }
                cardSocialHandle.text = handleText

                if (match.suggestedContactLookupKey != null) {
                    cardSuggestedContactHolder.beVisible()
                    cardNoSuggestion.beGone()
                    cardContactName.text = match.suggestedContactName
                    
                    if (match.suggestedContactPhotoUri?.isNotEmpty() == true) {
                        Glide.with(this@SocialMatchingActivity)
                            .load(match.suggestedContactPhotoUri)
                            .placeholder(org.fossify.commons.R.drawable.ic_person_vector)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .circleCrop()
                            .into(cardContactImage)
                    } else {
                        cardContactImage.setImageResource(org.fossify.commons.R.drawable.ic_person_vector)
                    }
                } else {
                    cardSuggestedContactHolder.beGone()
                    cardNoSuggestion.beVisible()
                }
            }
        }

        override fun getItemCount() = matches.size
    }
}
