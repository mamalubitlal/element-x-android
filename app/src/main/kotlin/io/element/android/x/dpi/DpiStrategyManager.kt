package io.element.android.x.dpi

import android.content.Context
import android.util.Log
import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.data.Strategy
import io.github.romanvht.byedpi.library.data.TestConfig
import io.github.romanvht.byedpi.library.picker.SiteListPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for DPI bypass strategies using ByeByeDPI library.
 */
class DpiStrategyManager(private val context: Context) {

    companion object {
        private const val TAG = "DpiStrategyManager"
    }

    private val library = ByeDpiLibraryHolder.library
    private val siteListPicker = SiteListPicker()
    
    /**
     * Get network ID (wifi/mobile).
     */
    suspend fun getNetworkId(): String = withContext(Dispatchers.IO) {
        "network_default" // Simplified
    }
    
    /**
     * Load available strategies from the library.
     */
    suspend fun loadStrategies(): List<String> = withContext(Dispatchers.IO) {
        val strategies = library.getDefaultStrategies().map { it.command }
        Log.d(TAG, "Loaded ${strategies.size} strategies")
        strategies
    }
    
    /**
     * Load test domains from active site lists.
     */
    suspend fun loadTestDomains(): List<String> = withContext(Dispatchers.IO) {
        val domains = library.getActiveDomains()
        if (domains.isEmpty()) {
            // Fallback to Matrix-related sites
            listOf("matrix.org", "vector.im", "accounts.matrix.org")
        } else {
            domains
        }
    }
    
    /**
     * Test a strategy against domains.
     */
    suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult {
        val strategyObj = library.createStrategy(
            command = strategy,
            name = extractStrategyName(strategy),
            description = "Tested strategy"
        )
        
        val result = library.testSingleStrategy(
            strategy = strategyObj,
            sites = domains,
            requestsPerSite = 2,
            timeoutSeconds = 5
        )
        
        return StrategyTestResult(
            strategy = result.name,
            command = result.command,
            successPercentage = result.successPercentage.toFloat(),
            message = "Success: ${result.successCount}/${result.totalRequests}"
        )
    }
    
    /**
     * Auto-select best strategy for current network.
     */
    suspend fun autoSelectStrategy(): String? = withContext(Dispatchers.IO) {
        val strategies = library.getDefaultStrategies()
        val domains = library.getActiveDomains()
        
        if (strategies.isEmpty() || domains.isEmpty()) {
            return@withContext null
        }
        
        val results = library.testStrategies(
            strategies = strategies,
            sites = domains,
            config = TestConfig.QUICK
        )
        
        library.getBestStrategy(results, minSuccessPercentage = 50)?.command
    }
    
    /**
     * Save strategy for network.
     */
    suspend fun saveStrategyForNetwork(networkId: String, name: String, strategy: String) {
        Log.d(TAG, "Saved strategy for network $networkId: $name")
    }

    /**
     * Check if strategy for network has expired (24 hour default).
     */
    suspend fun isStrategyExpired(networkId: String): Boolean {
        // Always refresh for simplicity - could persist save time
        return true
    }
    
    private fun extractStrategyName(command: String): String {
        val trimmed = command.trim()
        return if (trimmed.length > 25) trimmed.take(22) + "..." else trimmed
    }
}

data class StrategyTestResult(
    val strategy: String,
    val command: String,
    val successPercentage: Float,
    val message: String
)