package io.github.romanvht.byedpi.library.data

/**
 * Represents the result of checking a single site
 */
data class SiteCheckResult(
    val site: String,
    val successCount: Int,
    val totalCount: Int
) {
    val successPercentage: Int
        get() = if (totalCount > 0) (successCount * 100) / totalCount else 0
}
