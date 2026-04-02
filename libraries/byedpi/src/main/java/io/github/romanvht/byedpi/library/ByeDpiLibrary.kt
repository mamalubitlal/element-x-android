package io.github.romanvht.byedpi.library

import io.github.romanvht.byedpi.library.core.SiteChecker
import io.github.romanvht.byedpi.library.core.StrategyTester
import io.github.romanvht.byedpi.library.data.*
import io.github.romanvht.byedpi.library.server.ByeDpiServer
import io.github.romanvht.byedpi.library.server.ProxyConfig
import io.github.romanvht.byedpi.library.server.ServerResult
import io.github.romanvht.byedpi.library.server.ServerStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the ByeDPI Library
 * 
 * This class provides a clean API for:
 * - Managing the ByeDPI proxy server
 * - Testing and selecting bypass strategies
 * - Managing site lists
 * 
 * Example usage:
 * ```
 * val library = ByeDpiLibrary()
 * 
 * // Start the proxy server with a strategy
 * library.startServer(ProxyConfig.DEFAULT)
 * 
 * // Or find the best strategy first
 * val results = library.testWithDefaults()
 * val best = library.getBestStrategy(results)
 * 
 * // Start with the best strategy
 * library.startServer(ProxyConfig.fromCommand(best!!.command))
 * 
 * // Stop when done
 * library.stopServer()
 * ```
 */
class ByeDpiLibrary(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 1080
) {
    private val strategyTester = StrategyTester(proxyHost, proxyPort)
    private val siteChecker = SiteChecker(proxyHost, proxyPort)
    private val server = ByeDpiServer()
    
    /**
     * Whether a test is currently running
     */
    val isTesting: StateFlow<Boolean> = strategyTester.isRunning
    
    /**
     * Current strategy index being tested
     */
    val currentStrategyIndex: StateFlow<Int> = strategyTester.currentStrategyIndex
    
    /**
     * Overall testing progress (0.0 to 1.0)
     */
    val testingProgress: StateFlow<Float> = strategyTester.progress
    
    // ==================== Strategy Methods ====================
    
    /**
     * Get all default strategies
     */
    fun getDefaultStrategies(): List<Strategy> = DefaultStrategies.ALL
    
    /**
     * Get strategies by category
     */
    fun getStrategiesByCategory(category: StrategyCategory): List<Strategy> = 
        DefaultStrategies.getByCategory(category)
    
    /**
     * Parse custom strategies from a string
     * 
     * @param content String containing strategies (one per line)
     * @return List of parsed strategies
     */
    fun parseStrategies(content: String): List<Strategy> = 
        DefaultStrategies.fromString(content)
    
    /**
     * Create a single strategy
     */
    fun createStrategy(
        command: String,
        name: String = "",
        description: String = ""
    ): Strategy = Strategy(command = command, name = name, description = description)
    
    // ==================== Site List Methods ====================
    
    /**
     * Get all default site lists
     */
    fun getDefaultSiteLists(): List<SiteList> = DefaultSiteLists.ALL
    
    /**
     * Get active default site lists
     */
    fun getActiveSiteLists(): List<SiteList> = DefaultSiteLists.getActive()
    
    /**
     * Get a specific site list by ID
     */
    fun getSiteList(id: String): SiteList? = DefaultSiteLists.getById(id)
    
    /**
     * Get all domains from active site lists
     */
    fun getActiveDomains(): List<String> = DefaultSiteLists.getActiveDomains()
    
    /**
     * Create a custom site list
     */
    fun createSiteList(
        name: String,
        domains: List<String>,
        isActive: Boolean = true
    ): SiteList = DefaultSiteLists.createCustomList(name, domains, isActive)
    
    // ==================== Testing Methods ====================
    
    /**
     * Test all strategies against the provided sites
     * 
     * @param strategies List of strategies to test
     * @param sites List of sites (domains or URLs) to test against
     * @param config Test configuration
     * @param onStrategyStart Called when testing of a strategy starts (index, strategy)
     * @param onSiteChecked Called when a site check completes (strategyIndex, siteResult)
     * @param onStrategyComplete Called when a strategy completes (index, strategy)
     * @return List of strategies sorted by success percentage (best first)
     */
    suspend fun testStrategies(
        strategies: List<Strategy>,
        sites: List<String>,
        config: TestConfig = TestConfig.DEFAULT,
        onStrategyStart: ((Int, Strategy) -> Unit)? = null,
        onSiteChecked: ((Int, SiteCheckResult) -> Unit)? = null,
        onStrategyComplete: ((Int, Strategy) -> Unit)? = null
    ): List<Strategy> = strategyTester.testStrategies(
        strategies = strategies,
        sites = sites,
        config = config,
        onStrategyStart = onStrategyStart,
        onSiteChecked = onSiteChecked,
        onStrategyComplete = onStrategyComplete
    )
    
    /**
     * Test using default strategies and sites
     * 
     * @param siteListIds IDs of site lists to use (null = all active)
     * @param strategyCategory Strategy category to test (null = all)
     * @param config Test configuration
     * @param onProgress Callback for overall progress (0.0 to 1.0)
     * @return Results sorted by success percentage
     */
    suspend fun testWithDefaults(
        siteListIds: List<String>? = null,
        strategyCategory: StrategyCategory? = null,
        config: TestConfig = TestConfig.DEFAULT,
        onProgress: ((Float) -> Unit)? = null
    ): List<Strategy> {
        val strategies = strategyCategory?.let { getStrategiesByCategory(it) } ?: getDefaultStrategies()
        val sites = siteListIds?.let { ids ->
            ids.mapNotNull { getSiteList(it) }.flatMap { it.domains }.distinct()
        } ?: getActiveDomains()
        
        return testStrategies(
            strategies = strategies,
            sites = sites,
            config = config,
            onStrategyComplete = { _, _ ->
                onProgress?.invoke(strategyTester.progress.value)
            }
        )
    }
    
    /**
     * Test a single strategy
     * 
     * @param strategy The strategy to test
     * @param sites List of sites to test against
     * @param requestsPerSite Number of requests per site
     * @param timeoutSeconds Request timeout
     * @return Strategy with results
     */
    suspend fun testSingleStrategy(
        strategy: Strategy,
        sites: List<String>,
        requestsPerSite: Int = 1,
        timeoutSeconds: Long = 5
    ): Strategy {
        val results = testStrategies(
            strategies = listOf(strategy),
            sites = sites,
            config = TestConfig(
                delaySeconds = 0,
                requestsPerSite = requestsPerSite,
                requestTimeoutSeconds = timeoutSeconds
            )
        )
        return results.firstOrNull() ?: strategy
    }
    
    // ==================== Result Analysis Methods ====================
    
    /**
     * Get the best strategy from test results
     * 
     * @param results List of strategy results
     * @param minSuccessPercentage Minimum success percentage required (0-100)
     * @return The best strategy, or null if none meet the criteria
     */
    fun getBestStrategy(
        results: List<Strategy>,
        minSuccessPercentage: Int = 50
    ): Strategy? = strategyTester.getBestStrategy(results, minSuccessPercentage)
    
    /**
     * Get strategies that work well (above threshold)
     * 
     * @param results List of strategy results
     * @param threshold Success percentage threshold (0-100)
     * @return List of strategies above the threshold
     */
    fun getWorkingStrategies(
        results: List<Strategy>,
        threshold: Int = 70
    ): List<Strategy> = strategyTester.getWorkingStrategies(results, threshold)
    
    // ==================== Proxy Methods ====================
    
    /**
     * Test if the proxy is reachable
     * 
     * @param timeoutSeconds Timeout for the test
     * @return True if proxy is reachable
     */
    suspend fun testProxyConnection(timeoutSeconds: Long = 5): Boolean = 
        siteChecker.testProxyConnection(timeoutSeconds)
    
    /**
     * Stop the current testing process
     */
    fun stopTesting() {
        strategyTester.stop()
    }
    
    // ==================== Server Management Methods ====================
    
    /**
     * Get the server instance
     */
    fun getServer(): ByeDpiServer = server
    
    /**
     * Whether the server is currently running
     */
    val isServerRunning: Boolean get() = server.isRunning
    
    /**
     * Current server status
     */
    val serverStatus: ServerStatus get() = server.currentStatus
    
    /**
     * Start the ByeDPI proxy server
     * 
     * @param config Proxy configuration
     * @return Result of the start operation
     */
    suspend fun startServer(config: ProxyConfig = ProxyConfig.DEFAULT): ServerResult {
        return server.start(config)
    }
    
    /**
     * Start the server with a specific strategy
     * 
     * @param strategy The strategy to use
     * @param sniValue Optional SNI value to replace in the strategy
     * @return Result of the start operation
     */
    suspend fun startServerWithStrategy(
        strategy: Strategy,
        sniValue: String? = null
    ): ServerResult {
        val command = sniValue?.let { replaceSni(strategy.command, it) } ?: strategy.command
        val config = ProxyConfig.fromCommand(command)
        return server.start(config)
    }
    
    /**
     * Start the server using an external binary
     * 
     * @param binaryPath Path to the ByeDPI binary
     * @param config Proxy configuration
     * @return Result of the start operation
     */
    suspend fun startServerExternal(
        binaryPath: String,
        config: ProxyConfig = ProxyConfig.DEFAULT
    ): ServerResult {
        return server.startExternal(binaryPath, config)
    }
    
    /**
     * Stop the ByeDPI proxy server
     * 
     * @return Result of the stop operation
     */
    suspend fun stopServer(): ServerResult {
        return server.stop()
    }
    
    /**
     * Restart the server
     * 
     * @param config New configuration (null to use current)
     * @return Result of the restart operation
     */
    suspend fun restartServer(config: ProxyConfig? = null): ServerResult {
        return server.restart(config)
    }
    
    /**
     * Check if the server is responsive
     */
    fun pingServer(): Boolean {
        return server.ping()
    }
    
    /**
     * Check if native library is available
     */
    fun isNativeLibraryAvailable(): Boolean {
        return server.isNativeLibraryAvailable()
    }
    
    /**
     * Find and start the best strategy automatically
     * 
     * @param siteListIds IDs of site lists to test (null = all active)
     * @param minSuccessPercentage Minimum success percentage required
     * @param onProgress Progress callback
     * @return The best strategy found, or null if none work
     */
    suspend fun autoStartBestStrategy(
        siteListIds: List<String>? = null,
        minSuccessPercentage: Int = 50,
        onProgress: ((Float, String) -> Unit)? = null
    ): Strategy? {
        val results = testWithDefaults(
            siteListIds = siteListIds,
            onProgress = { progress ->
                onProgress?.invoke(progress, "Testing strategies...")
            }
        )
        
        val best = getBestStrategy(results, minSuccessPercentage)
        if (best != null) {
            startServerWithStrategy(best)
        }
        return best
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Replace {sni} placeholder in a strategy command
     * 
     * @param command The strategy command
     * @param sniValue The SNI value to use
     * @return Command with {sni} replaced
     */
    fun replaceSni(command: String, sniValue: String): String = 
        command.replace("{sni}", sniValue)
}

/**
 * Create a new ByeDpiLibrary instance with the specified proxy settings
 */
fun createByeDpiLibrary(proxyHost: String = "127.0.0.1", proxyPort: Int = 1080): ByeDpiLibrary =
    ByeDpiLibrary(proxyHost, proxyPort)
