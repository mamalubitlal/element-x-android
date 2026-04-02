package io.github.romanvht.byedpi.library.data

/**
 * Default site lists for testing bypass strategies
 */
object DefaultSiteLists {
    
    /**
     * YouTube related domains
     */
    val YOUTUBE = SiteList(
        id = "youtube",
        name = "YouTube",
        domains = listOf(
            "youtu.be",
            "youtube.com",
            "i.ytimg.com",
            "i9.ytimg.com",
            "yt3.ggpht.com",
            "yt4.ggpht.com",
            "googleapis.com",
            "jnn-pa.googleapis.com",
            "googleusercontent.com",
            "signaler-pa.youtube.com",
            "youtubei.googleapis.com",
            "manifest.googlevideo.com",
            "yt3.googleusercontent.com"
        ),
        isActive = true,
        isBuiltIn = true
    )
    
    /**
     * Google Video related domains
     */
    val GOOGLE_VIDEO = SiteList(
        id = "googlevideo",
        name = "Google Video",
        domains = listOf(
            "googlevideo.com",
            "rr*.googlevideo.com",
            "*.googlevideo.com"
        ),
        isActive = true,
        isBuiltIn = true
    )
    
    /**
     * Discord related domains
     */
    val DISCORD = SiteList(
        id = "discord",
        name = "Discord",
        domains = listOf(
            "discord.com",
            "discordapp.com",
            "discord.gg",
            "discordmedia.net",
            "discordapp.net",
            "discordstatus.com",
            "*.discord.com",
            "*.discordapp.com"
        ),
        isActive = false,
        isBuiltIn = true
    )
    
    /**
     * Cloudflare related domains
     */
    val CLOUDFLARE = SiteList(
        id = "cloudflare",
        name = "Cloudflare",
        domains = listOf(
            "cloudflare.com",
            "cdnjs.cloudflare.com",
            "clients*.cloudflare.com",
            "*.cloudflare.com"
        ),
        isActive = false,
        isBuiltIn = true
    )
    
    /**
     * Social media domains
     */
    val SOCIAL = SiteList(
        id = "social",
        name = "Social Media",
        domains = listOf(
            "twitter.com",
            "x.com",
            "facebook.com",
            "instagram.com",
            "tiktok.com",
            "reddit.com",
            "twitch.tv",
            "vk.com"
        ),
        isActive = false,
        isBuiltIn = true
    )
    
    /**
     * General purpose sites
     */
    val GENERAL = SiteList(
        id = "general",
        name = "General Sites",
        domains = listOf(
            "google.com",
            "wikipedia.org",
            "github.com",
            "stackoverflow.com",
            "microsoft.com",
            "apple.com",
            "amazon.com"
        ),
        isActive = false,
        isBuiltIn = true
    )
    
    /**
     * All default site lists
     */
    val ALL = listOf(
        YOUTUBE,
        GOOGLE_VIDEO,
        DISCORD,
        CLOUDFLARE,
        SOCIAL,
        GENERAL
    )
    
    /**
     * Get all active default site lists
     */
    fun getActive(): List<SiteList> = ALL.filter { it.isActive }
    
    /**
     * Get all domains from active lists
     */
    fun getActiveDomains(): List<String> = getActive()
        .flatMap { it.domains }
        .distinct()
    
    /**
     * Get a specific site list by ID
     */
    fun getById(id: String): SiteList? = ALL.find { it.id == id }
    
    /**
     * Create a custom site list
     */
    fun createCustomList(
        name: String,
        domains: List<String>,
        isActive: Boolean = true
    ): SiteList {
        val id = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return SiteList(
            id = id,
            name = name,
            domains = domains,
            isActive = isActive,
            isBuiltIn = false
        )
    }
}

/**
 * Site list category for filtering
 */
enum class SiteListCategory {
    VIDEO,
    GAMING,
    SOCIAL,
    CLOUD,
    GENERAL,
    ALL_CATEGORIES
}
