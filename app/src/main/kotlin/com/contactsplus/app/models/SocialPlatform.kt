package com.contactsplus.app.models

import com.contactsplus.app.R

enum class SocialPlatform(
    val displayName: String,
    val iconRes: Int,
    val deeplinkTemplate: String,
    val webUrlTemplate: String,
    val packageName: String
) {
    INSTAGRAM(
        displayName = "Instagram",
        iconRes = R.drawable.ic_instagram,
        deeplinkTemplate = "instagram://user?username={username}",
        webUrlTemplate = "https://instagram.com/{username}",
        packageName = "com.instagram.android"
    ),
    SNAPCHAT(
        displayName = "Snapchat",
        iconRes = R.drawable.ic_snapchat,
        deeplinkTemplate = "snapchat://add/{username}",
        webUrlTemplate = "https://snapchat.com/add/{username}",
        packageName = "com.snapchat.android"
    ),
    WHATSAPP(
        displayName = "WhatsApp",
        iconRes = R.drawable.ic_whatsapp,
        deeplinkTemplate = "https://wa.me/{username}",
        webUrlTemplate = "https://wa.me/{username}",
        packageName = "com.whatsapp"
    ),
    TELEGRAM(
        displayName = "Telegram",
        iconRes = R.drawable.ic_telegram,
        deeplinkTemplate = "tg://resolve?domain={username}",
        webUrlTemplate = "https://t.me/{username}",
        packageName = "org.telegram.messenger"
    ),
    DISCORD(
        displayName = "Discord",
        iconRes = R.drawable.ic_discord,
        deeplinkTemplate = "discord://users/{username}",
        webUrlTemplate = "https://discord.com/users/{username}",
        packageName = "com.discord"
    ),
    TIKTOK(
        displayName = "TikTok",
        iconRes = R.drawable.ic_tiktok,
        deeplinkTemplate = "snssdk1128://user/profile/{username}",
        webUrlTemplate = "https://tiktok.com/@{username}",
        packageName = "com.ss.android.ugc.tiktok"
    ),
    TWITTER(
        displayName = "X (Twitter)",
        iconRes = R.drawable.ic_twitter,
        deeplinkTemplate = "twitter://user?screen_name={username}",
        webUrlTemplate = "https://x.com/{username}",
        packageName = "com.twitter.android"
    ),
    LINKEDIN(
        displayName = "LinkedIn",
        iconRes = R.drawable.ic_linkedin,
        deeplinkTemplate = "linkedin://profile/{username}",
        webUrlTemplate = "https://linkedin.com/in/{username}",
        packageName = "com.linkedin.android"
    ),
    SIGNAL(
        displayName = "Signal",
        iconRes = R.drawable.ic_signal,
        deeplinkTemplate = "sgnl://signal.me/#p/{username}",
        webUrlTemplate = "https://signal.me/#p/{username}",
        packageName = "org.thoughtcrime.securesms"
    ),
    MESSENGER(
        displayName = "Messenger",
        iconRes = R.drawable.ic_messenger,
        deeplinkTemplate = "fb-messenger://user/{username}",
        webUrlTemplate = "https://m.me/{username}",
        packageName = "com.facebook.orca"
    ),
    THREADS(
        displayName = "Threads",
        iconRes = R.drawable.ic_threads,
        deeplinkTemplate = "barcelona://user?username={username}",
        webUrlTemplate = "https://threads.net/@{username}",
        packageName = "com.instagram.barcelona"
    ),
    CUSTOM(
        displayName = "Custom Link",
        iconRes = R.drawable.ic_social_link,
        deeplinkTemplate = "{username}",
        webUrlTemplate = "{username}",
        packageName = ""
    );

    fun buildDeeplink(username: String): String = deeplinkTemplate.replace("{username}", username)
    fun buildWebUrl(username: String): String = webUrlTemplate.replace("{username}", username)

    companion object {
        fun fromDisplayName(name: String): SocialPlatform? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}
