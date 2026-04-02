package io.github.romanvht.byedpi.library.picker

import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Site list picker for managing and selecting site lists for testing
 * 
 * This class provides an easy way to:
 * - Browse default site lists
 * - Add custom site lists
 * - Activate/deactivate site lists
 * - Get domains from selected lists
 * 
 * Example usage:
 * ```
 * val picker = SiteListPicker()
 * 
 * // Get available lists
 * val lists = picker.getAvailableLists()
 * 
 * // Activate specific lists
 * picker.activateList("youtube")
 * picker.activateList("discord")
 * 
 * // Get all domains from active lists
 * val domains = picker.getActiveDomains()
 * 
 * // Add custom list
 * picker.addCustomList("My Sites", listOf("example.com", "test.com"))
 * ```
 */
class SiteListPicker(
    private val library: ByeDpiLibrary = ByeDpiLibrary()
) {
    private val customLists = mutableListOf<SiteList>()
    private val activeListIds = mutableSetOf<String>()
    
    private val _availableLists = MutableStateFlow<List<SiteList>>(emptyList())
    val availableLists: StateFlow<List<SiteList>> = _availableLists.asStateFlow()
    
    init {
        // Initialize with default active lists
        library.getActiveSiteLists().forEach { list ->
            activeListIds.add(list.id)
        }
        refreshAvailable()
    }
    
    /**
     * Get all available site lists (default + custom)
     */
    fun getAvailableLists(): List<SiteList> {
        val defaults = library.getDefaultSiteLists().map { list ->
            list.copy(isActive = activeListIds.contains(list.id))
        }
        return defaults + customLists
    }
    
    /**
     * Get only default site lists
     */
    fun getDefaultLists(): List<SiteList> = library.getDefaultSiteLists()
    
    /**
     * Get only custom site lists
     */
    fun getCustomLists(): List<SiteList> = customLists.toList()
    
    /**
     * Get active site lists
     */
    fun getActiveLists(): List<SiteList> = getAvailableLists().filter { it.isActive }
    
    /**
     * Get all domains from active site lists
     */
    fun getActiveDomains(): List<String> = getActiveLists()
        .flatMap { it.domains }
        .distinct()
    
    /**
     * Get a specific site list by ID
     */
    fun getList(id: String): SiteList? = getAvailableLists().find { it.id == id }
    
    /**
     * Activate a site list
     * 
     * @param listId The ID of the list to activate
     * @return True if activated, false if not found
     */
    fun activateList(listId: String): Boolean {
        val list = getAvailableLists().find { it.id == listId } ?: return false
        activeListIds.add(listId)
        refreshAvailable()
        return true
    }
    
    /**
     * Deactivate a site list
     * 
     * @param listId The ID of the list to deactivate
     * @return True if deactivated, false if not found
     */
    fun deactivateList(listId: String): Boolean {
        if (!activeListIds.contains(listId)) return false
        activeListIds.remove(listId)
        refreshAvailable()
        return true
    }
    
    /**
     * Toggle activation state of a site list
     */
    fun toggleList(listId: String): Boolean {
        return if (activeListIds.contains(listId)) {
            deactivateList(listId)
        } else {
            activateList(listId)
        }
    }
    
    /**
     * Activate multiple lists at once
     */
    fun activateLists(listIds: List<String>) {
        listIds.forEach { activeListIds.add(it) }
        refreshAvailable()
    }
    
    /**
     * Activate all lists
     */
    fun activateAll() {
        getAvailableLists().forEach { activeListIds.add(it.id) }
        refreshAvailable()
    }
    
    /**
     * Deactivate all lists
     */
    fun deactivateAll() {
        activeListIds.clear()
        refreshAvailable()
    }
    
    /**
     * Check if a list is active
     */
    fun isActive(listId: String): Boolean = activeListIds.contains(listId)
    
    /**
     * Add a custom site list
     * 
     * @param name The name of the list
     * @param domains List of domains
     * @param isActive Whether to activate immediately
     * @return The created site list
     */
    fun addCustomList(
        name: String,
        domains: List<String>,
        isActive: Boolean = true
    ): SiteList {
        val list = library.createSiteList(name, domains, isActive)
        customLists.add(list)
        if (isActive) {
            activeListIds.add(list.id)
        }
        refreshAvailable()
        return list
    }
    
    /**
     * Remove a custom site list
     * 
     * @param listId The ID of the list to remove
     * @return True if removed, false if not found or is default
     */
    fun removeCustomList(listId: String): Boolean {
        val index = customLists.indexOfFirst { it.id == listId }
        if (index == -1) return false
        customLists.removeAt(index)
        activeListIds.remove(listId)
        refreshAvailable()
        return true
    }
    
    /**
     * Update a custom site list
     * 
     * @param listId The ID of the list to update
     * @param name New name (null to keep current)
     * @param domains New domains (null to keep current)
     * @return True if updated, false if not found
     */
    fun updateCustomList(
        listId: String,
        name: String? = null,
        domains: List<String>? = null
    ): Boolean {
        val index = customLists.indexOfFirst { it.id == listId }
        if (index == -1) return false
        
        val old = customLists[index]
        customLists[index] = old.copy(
            name = name ?: old.name,
            domains = domains ?: old.domains
        )
        refreshAvailable()
        return true
    }
    
    /**
     * Clear all custom lists
     */
    fun clearCustomLists() {
        customLists.forEach { activeListIds.remove(it.id) }
        customLists.clear()
        refreshAvailable()
    }
    
    /**
     * Search lists by name
     */
    fun search(query: String): List<SiteList> {
        val lowerQuery = query.lowercase()
        return getAvailableLists().filter { list ->
            list.name.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get lists that contain a specific domain
     */
    fun findListsContainingDomain(domain: String): List<SiteList> {
        val lowerDomain = domain.lowercase()
        return getAvailableLists().filter { list ->
            list.domains.any { it.lowercase().contains(lowerDomain) }
        }
    }
    
    private fun refreshAvailable() {
        _availableLists.value = getAvailableLists()
    }
}
