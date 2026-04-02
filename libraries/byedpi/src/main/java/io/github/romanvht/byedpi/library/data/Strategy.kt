package io.github.romanvht.byedpi.library.data

/**
 * Represents a bypass strategy with its command-line arguments and test results
 */
data class Strategy(
    val command: String,
    val name: String = "",
    val description: String = "",
    var successCount: Int = 0,
    var totalRequests: Int = 0,
    var currentProgress: Int = 0,
    var isCompleted: Boolean = false,
    val siteResults: MutableList<SiteCheckResult> = mutableListOf()
) {
    val successPercentage: Int
        get() = if (totalRequests > 0) (successCount * 100) / totalRequests else 0
    
    /**
     * Creates a copy of this strategy with reset results
     */
    fun reset(): Strategy = Strategy(
        command = command,
        name = name,
        description = description
    )
}
