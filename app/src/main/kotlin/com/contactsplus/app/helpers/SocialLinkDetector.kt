package com.contactsplus.app.helpers

import com.contactsplus.app.models.SocialLink
import com.contactsplus.app.models.SocialPlatform
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.IM
import java.util.regex.Pattern

/**
 * Automatically detects social media links from contact data.
 * Parses notes, websites, and IM fields to extract social handles.
 */
object SocialLinkDetector {

    // Patterns for detecting usernames in text
    private val USERNAME_PATTERN = Pattern.compile("@([A-Za-z0-9_.-]+)")

    // URL patterns for various social platforms
    private val SOCIAL_URL_PATTERNS = mapOf(
        SocialPlatform.INSTAGRAM to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?instagram\\.com/([A-Za-z0-9_.-]+)/?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:https?://)?(?:www\\.)?instagr\\.am/([A-Za-z0-9_.-]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.TWITTER to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?twitter\\.com/([A-Za-z0-9_]+)/?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:https?://)?(?:www\\.)?x\\.com/([A-Za-z0-9_]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.TIKTOK to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?tiktok\\.com/@([A-Za-z0-9_.-]+)/?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:https?://)?(?:vm\\.)?tiktok\\.com/([A-Za-z0-9]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.LINKEDIN to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/([A-Za-z0-9_-]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.SNAPCHAT to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?snapchat\\.com/add/([A-Za-z0-9_.-]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.DISCORD to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?discord\\.gg/([A-Za-z0-9]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.TELEGRAM to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?t\\.me/([A-Za-z0-9_]+)/?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:https?://)?(?:www\\.)?telegram\\.me/([A-Za-z0-9_]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.WHATSAPP to listOf(
            Pattern.compile("(?:https?://)?(?:wa\\.me|api\\.whatsapp\\.com/send\\?phone=)(\\+?[0-9]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.MESSENGER to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?m\\.me/([A-Za-z0-9.]+)/?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:https?://)?(?:www\\.)?messenger\\.com/t/([A-Za-z0-9.]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.THREADS to listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?threads\\.net/@([A-Za-z0-9_.-]+)/?", Pattern.CASE_INSENSITIVE)
        ),
        SocialPlatform.SIGNAL to listOf(
            Pattern.compile("(?:https?://)?signal\\.me/#p/(\\+?[0-9]+)", Pattern.CASE_INSENSITIVE)
        )
    )

    // IM type to social platform mapping
    private val IM_TYPE_MAPPING = mapOf(
        "Telegram" to SocialPlatform.TELEGRAM,
        "WhatsApp" to SocialPlatform.WHATSAPP,
        "Skype" to SocialPlatform.DISCORD, // Closest match
        "Signal" to SocialPlatform.SIGNAL
    )

    // Keywords that suggest a specific platform when found near a username
    private val PLATFORM_KEYWORDS = mapOf(
        SocialPlatform.INSTAGRAM to listOf("instagram", "insta", "ig"),
        SocialPlatform.TWITTER to listOf("twitter", "tweet", "x.com"),
        SocialPlatform.TIKTOK to listOf("tiktok", "tik tok"),
        SocialPlatform.SNAPCHAT to listOf("snapchat", "snap", "sc"),
        SocialPlatform.LINKEDIN to listOf("linkedin", "linked in"),
        SocialPlatform.DISCORD to listOf("discord"),
        SocialPlatform.TELEGRAM to listOf("telegram", "tg"),
        SocialPlatform.THREADS to listOf("threads")
    )

    data class DetectedLink(
        val platform: SocialPlatform,
        val username: String,
        val source: String, // "notes", "website", "im"
        val confidence: Float // 0.0 to 1.0
    )

    /**
     * Detect social links from a contact's data.
     * Returns a list of potential social links with confidence scores.
     */
    fun detectFromContact(contact: Contact, contactId: String): List<DetectedLink> {
        val detectedLinks = mutableListOf<DetectedLink>()

        // 1. Parse websites for social media URLs
        contact.websites.forEach { website ->
            detectFromUrl(website)?.let { detectedLinks.add(it) }
        }

        // 2. Parse notes for @usernames and URLs
        if (contact.notes.isNotBlank()) {
            detectedLinks.addAll(detectFromNotes(contact.notes))
        }

        // 3. Parse IM fields
        contact.IMs.forEach { im ->
            detectFromIM(im)?.let { detectedLinks.add(it) }
        }

        // Remove duplicates (same platform + username)
        return detectedLinks.distinctBy { "${it.platform.name}:${it.username.lowercase()}" }
    }

    /**
     * Detect social link from a URL.
     */
    fun detectFromUrl(url: String): DetectedLink? {
        val cleanUrl = url.trim()

        for ((platform, patterns) in SOCIAL_URL_PATTERNS) {
            for (pattern in patterns) {
                val matcher = pattern.matcher(cleanUrl)
                if (matcher.find()) {
                    val username = matcher.group(1) ?: continue
                    return DetectedLink(
                        platform = platform,
                        username = username,
                        source = "website",
                        confidence = 1.0f
                    )
                }
            }
        }
        return null
    }

    /**
     * Detect social links from notes text.
     */
    fun detectFromNotes(notes: String): List<DetectedLink> {
        val detectedLinks = mutableListOf<DetectedLink>()
        val lowerNotes = notes.lowercase()

        // First, try to find URLs in notes
        SOCIAL_URL_PATTERNS.forEach { (platform, patterns) ->
            patterns.forEach { pattern ->
                val matcher = pattern.matcher(notes)
                while (matcher.find()) {
                    val username = matcher.group(1) ?: return@forEach
                    detectedLinks.add(DetectedLink(
                        platform = platform,
                        username = username,
                        source = "notes",
                        confidence = 1.0f
                    ))
                }
            }
        }

        // Then, look for @usernames with platform context
        val usernameMatcher = USERNAME_PATTERN.matcher(notes)
        while (usernameMatcher.find()) {
            val username = usernameMatcher.group(1) ?: continue
            val startIndex = maxOf(0, usernameMatcher.start() - 50)
            val endIndex = minOf(notes.length, usernameMatcher.end() + 50)
            val context = notes.substring(startIndex, endIndex).lowercase()

            // Try to determine platform from context
            var detectedPlatform: SocialPlatform? = null
            var maxConfidence = 0.5f

            for ((platform, keywords) in PLATFORM_KEYWORDS) {
                for (keyword in keywords) {
                    if (context.contains(keyword)) {
                        detectedPlatform = platform
                        maxConfidence = 0.8f
                        break
                    }
                }
                if (detectedPlatform != null) break
            }

            // If no platform detected from context, suggest Instagram as most common
            if (detectedPlatform == null) {
                detectedPlatform = SocialPlatform.INSTAGRAM
                maxConfidence = 0.4f
            }

            detectedLinks.add(DetectedLink(
                platform = detectedPlatform,
                username = username,
                source = "notes",
                confidence = maxConfidence
            ))
        }

        return detectedLinks
    }

    /**
     * Detect social link from IM field.
     */
    fun detectFromIM(im: IM): DetectedLink? {
        val imType = im.label.ifEmpty { im.type.toString() }

        // Check if IM type maps to a social platform
        for ((keyword, platform) in IM_TYPE_MAPPING) {
            if (imType.contains(keyword, ignoreCase = true)) {
                return DetectedLink(
                    platform = platform,
                    username = im.value,
                    source = "im",
                    confidence = 0.9f
                )
            }
        }
        return null
    }

    /**
     * Convert detected links to SocialLink entities.
     */
    fun toSocialLinks(detectedLinks: List<DetectedLink>, contactId: String): List<SocialLink> {
        return detectedLinks.map { detected ->
            SocialLink(
                contactLookupKey = contactId,
                platform = detected.platform,
                username = detected.username,
                customLabel = if (detected.confidence < 0.7f) "Auto-detected" else null
            )
        }
    }

    /**
     * Get a user-friendly description of where the link was detected.
     */
    fun getSourceDescription(source: String): String {
        return when (source) {
            "website" -> "From websites"
            "notes" -> "From notes"
            "im" -> "From messaging apps"
            else -> "Detected"
        }
    }
}
