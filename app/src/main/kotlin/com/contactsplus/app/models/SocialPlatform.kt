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

        fun detect(text: String): Pair<SocialPlatform, String>? {
            val url = text.trim()
            return when {
                url.contains("instagram.com/") -> {
                    val username = url.substringAfter("instagram.com/").substringBefore("?").substringBefore("/")
                    INSTAGRAM to username
                }
                url.contains("snapchat.com/add/") -> {
                    val username = url.substringAfter("snapchat.com/add/").substringBefore("?").substringBefore("/")
                    SNAPCHAT to username
                }
                url.contains("wa.me/") -> {
                    val username = url.substringAfter("wa.me/").substringBefore("?").substringBefore("/")
                    WHATSAPP to username
                }
                url.contains("t.me/") -> {
                    val username = url.substringAfter("t.me/").substringBefore("?").substringBefore("/")
                    TELEGRAM to username
                }
                url.contains("discord.com/users/") -> {
                    val username = url.substringAfter("discord.com/users/").substringBefore("?").substringBefore("/")
                    DISCORD to username
                }
                url.contains("tiktok.com/@") -> {
                    val username = url.substringAfter("tiktok.com/@").substringBefore("?").substringBefore("/")
                    TIKTOK to username
                }
                url.contains("x.com/") || url.contains("twitter.com/") -> {
                    val temp = if (url.contains("x.com/")) url.substringAfter("x.com/") else url.substringAfter("twitter.com/")
                    val username = temp.substringBefore("?").substringBefore("/")
                    TWITTER to username
                }
                url.contains("linkedin.com/in/") -> {
                    val username = url.substringAfter("linkedin.com/in/").substringBefore("?").substringBefore("/")
                    LINKEDIN to username
                }
                url.contains("signal.me/#p/") -> {
                    val username = url.substringAfter("signal.me/#p/").substringBefore("?").substringBefore("/")
                    SIGNAL to username
                }
                url.contains("m.me/") -> {
                    val username = url.substringAfter("m.me/").substringBefore("?").substringBefore("/")
                    MESSENGER to username
                }
                url.contains("threads.net/@") -> {
                    val username = url.substringAfter("threads.net/@").substringBefore("?").substringBefore("/")
                    THREADS to username
                }
                else -> null
            }
        }
    }
}
