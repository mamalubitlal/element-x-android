package io.github.romanvht.byedpi.library.data

/**
 * Represents a list of domains to test
 */
data class SiteList(
    val id: String,
    val name: String,
    val domains: List<String>,
    val isActive: Boolean = true,
    val isBuiltIn: Boolean = false
) {
    /**
     * Creates a copy with the specified active state
     */
    fun withActiveState(active: Boolean): SiteList = copy(isActive = active)
}
