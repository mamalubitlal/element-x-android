package io.github.romanvht.byedpi.library.picker

import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Strategy picker for selecting and managing bypass strategies
 * 
 * This class provides an easy way to:
 * - Browse default strategies
 * - Filter by category
 * - Add custom strategies
 * - Track selection state
 * 
 * Example usage:
 * ```
 * val picker = StrategyPicker()
 * 
 * // Get available strategies
 * val strategies = picker.getAvailableStrategies()
 * 
 * // Filter by category
 * val sniStrategies = picker.filterByCategory(StrategyCategory.SNI_BASED)
 * 
 * // Add custom strategy
 * picker.addCustomStrategy("-d1 -s1 -a1", "My Custom Strategy")
 * 
 * // Select a strategy
 * picker.selectStrategy(strategy)
 * ```
 */
class StrategyPicker(
    private val library: ByeDpiLibrary = ByeDpiLibrary()
) {
    private val customStrategies = mutableListOf<Strategy>()
    private val selectedStrategies = mutableListOf<Strategy>()
    
    private val _availableStrategies = MutableStateFlow<List<Strategy>>(emptyList())
    val availableStrategies: StateFlow<List<Strategy>> = _availableStrategies.asStateFlow()

    private val _selectedStrategy = MutableStateFlow<Strategy?>(null)
    val selectedStrategy: StateFlow<Strategy?> = _selectedStrategy.asStateFlow()

    init {
        refreshAvailable()
    }

    /**
     * Get all available strategies (default + custom)
     */
    fun getAvailableStrategies(): List<Strategy> = 
        library.getDefaultStrategies() + customStrategies
    
    /**
     * Get only default strategies
     */
    fun getDefaultStrategies(): List<Strategy> = library.getDefaultStrategies()
    
    /**
     * Get only custom strategies
     */
    fun getCustomStrategies(): List<Strategy> = customStrategies.toList()
    
    /**
     * Filter strategies by category
     */
    fun filterByCategory(category: StrategyCategory): List<Strategy> = 
        library.getStrategiesByCategory(category)
    
    /**
     * Search strategies by name or command
     */
    fun search(query: String): List<Strategy> {
        val lowerQuery = query.lowercase()
        return getAvailableStrategies().filter { strategy ->
            strategy.name.lowercase().contains(lowerQuery) ||
            strategy.command.lowercase().contains(lowerQuery) ||
            strategy.description.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Add a custom strategy
     * 
     * @param command The ByeDPI command
     * @param name Strategy name
     * @param description Strategy description
     * @return The created strategy
     */
    fun addCustomStrategy(
        command: String,
        name: String = "Custom Strategy ${customStrategies.size + 1}",
        description: String = ""
    ): Strategy {
        val strategy = library.createStrategy(command, name, description)
        customStrategies.add(strategy)
        refreshAvailable()
        return strategy
    }
    
    /**
     * Add multiple custom strategies from a string
     * 
     * @param content String containing strategies (one per line)
     * @return List of added strategies
     */
    fun addCustomStrategies(content: String): List<Strategy> {
        val strategies = library.parseStrategies(content)
        customStrategies.addAll(strategies)
        refreshAvailable()
        return strategies
    }
    
    /**
     * Remove a custom strategy
     * 
     * @param strategy The strategy to remove
     * @return True if removed, false if not found or is default
     */
    fun removeCustomStrategy(strategy: Strategy): Boolean {
        val removed = customStrategies.remove(strategy)
        if (removed) {
            selectedStrategies.remove(strategy)
            refreshAvailable()
        }
        return removed
    }
    
    /**
     * Clear all custom strategies
     */
    fun clearCustomStrategies() {
        customStrategies.clear()
        refreshAvailable()
    }
    
    /**
     * Select a strategy
     */
    fun selectStrategy(strategy: Strategy) {
        _selectedStrategy.value = strategy
        if (!selectedStrategies.contains(strategy)) {
            selectedStrategies.add(strategy)
        }
    }
    
    /**
     * Deselect a strategy
     */
    fun deselectStrategy(strategy: Strategy) {
        selectedStrategies.remove(strategy)
        if (_selectedStrategy.value == strategy) {
            _selectedStrategy.value = selectedStrategies.lastOrNull()
        }
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        selectedStrategies.clear()
        _selectedStrategy.value = null
    }
    
    /**
     * Get selected strategies
     */
    fun getSelectedStrategies(): List<Strategy> = selectedStrategies.toList()
    
    /**
     * Get the currently selected strategy
     */
    fun getSelected(): Strategy? = _selectedStrategy.value
    
    /**
     * Toggle selection of a strategy
     */
    fun toggleSelection(strategy: Strategy) {
        if (selectedStrategies.contains(strategy)) {
            deselectStrategy(strategy)
        } else {
            selectStrategy(strategy)
        }
    }
    
    /**
     * Check if a strategy is selected
     */
    fun isSelected(strategy: Strategy): Boolean = selectedStrategies.contains(strategy)
    
    /**
     * Replace {sni} placeholder in all available strategies
     * 
     * @param sniValue The SNI value to use
     * @return List of strategies with SNI replaced
     */
    fun withSniValue(sniValue: String): List<Strategy> = 
        getAvailableStrategies().map { strategy ->
            strategy.copy(
                command = strategy.command.replace("{sni}", sniValue)
            )
        }
    
    /**
     * Get strategies that don't require SNI
     */
    fun getNonSniStrategies(): List<Strategy> = 
        getAvailableStrategies().filter { !it.command.contains("{sni}") }
    
    /**
     * Get strategies that require SNI
     */
    fun getSniStrategies(): List<Strategy> = 
        getAvailableStrategies().filter { it.command.contains("{sni}") }
    
    /**
     * Sort strategies by a given criteria
     */
    fun sortBy(criteria: StrategySortCriteria): List<Strategy> {
        val strategies = getAvailableStrategies()
        return when (criteria) {
            StrategySortCriteria.NAME -> strategies.sortedBy { it.name }
            StrategySortCriteria.COMMAND_LENGTH -> strategies.sortedBy { it.command.length }
            StrategySortCriteria.COMMAND_COMPLEXITY -> strategies.sortedByDescending { 
                it.command.count { c -> c == '-' } 
            }
        }
    }
    
    private fun refreshAvailable() {
        _availableStrategies.value = getAvailableStrategies()
    }
}

/**
 * Criteria for sorting strategies
 */
enum class StrategySortCriteria {
    NAME,
    COMMAND_LENGTH,
    COMMAND_COMPLEXITY
}
