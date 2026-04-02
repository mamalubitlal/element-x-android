package io.github.romanvht.byedpi.library.core

import io.github.romanvht.byedpi.library.data.Strategy
import io.github.romanvht.byedpi.library.data.SiteCheckResult
import io.github.romanvht.byedpi.library.data.TestConfig
import io.github.romanvht.byedpi.library.data.SiteList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Main class for testing bypass strategies
 */
class StrategyTester(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 1080
) {
    private val siteChecker = SiteChecker(proxyHost, proxyPort)
    private var testJob: Job? = null
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentStrategyIndex = MutableStateFlow(-1)
    val currentStrategyIndex: StateFlow<Int> = _currentStrategyIndex.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    /**
     * Test all strategies against the provided sites
     * 
     * @param strategies List of strategies to test
     * @param sites List of sites to test against
     * @param config Test configuration
     * @param onStrategyStart Called when testing of a strategy starts (index, strategy)
     * @param onSiteChecked Called when a site check completes (strategyIndex, siteResult)
     * @param onStrategyComplete Called when a strategy completes (index, strategy)
     * @return List of strategies sorted by success percentage
     */
    suspend fun testStrategies(
        strategies: List<Strategy>,
        sites: List<String>,
        config: TestConfig = TestConfig.DEFAULT,
        onStrategyStart: ((Int, Strategy) -> Unit)? = null,
        onSiteChecked: ((Int, SiteCheckResult) -> Unit)? = null,
        onStrategyComplete: ((Int, Strategy) -> Unit)? = null
    ): List<Strategy> = withContext(Dispatchers.IO) {
        if (strategies.isEmpty() || sites.isEmpty()) {
            return@withContext emptyList()
        }
        
        _isRunning.value = true
        val results = mutableListOf<Strategy>()
        val totalStrategies = strategies.size
        
        try {
            for ((index, strategy) in strategies.withIndex()) {
                if (!_isRunning.value) break
                
                _currentStrategyIndex.value = index
                onStrategyStart?.invoke(index, strategy)
                
                // Replace {sni} placeholder with actual SNI value
                val command = strategy.command.replace("{sni}", config.sniValue)
                
                // Reset strategy for this test
                val testStrategy = strategy.copy(
                    command = command,
                    successCount = 0,
                    totalRequests = sites.size * config.requestsPerSite,
                    currentProgress = 0,
                    isCompleted = false,
                    siteResults = mutableListOf()
                )
                
                // Check all sites with this strategy
                val siteResults = siteChecker.checkSitesAsync(
                    sites = sites,
                    requestsCount = config.requestsPerSite,
                    requestTimeout = config.requestTimeoutSeconds,
                    concurrentRequests = config.maxConcurrentRequests
                ) { site, successCount, totalRequests ->
                    val siteResult = SiteCheckResult(site, successCount, totalRequests)
                    testStrategy.siteResults.add(siteResult)
                    testStrategy.successCount += successCount
                    testStrategy.currentProgress += totalRequests
                    onSiteChecked?.invoke(index, siteResult)
                }
                
                testStrategy.isCompleted = true
                onStrategyComplete?.invoke(index, testStrategy)
                results.add(testStrategy)
                
                // Update overall progress
                _progress.value = (index + 1).toFloat() / totalStrategies
                
                // Delay between strategies if not the last one
                if (index < strategies.size - 1 && config.delaySeconds > 0) {
                    kotlinx.coroutines.delay(config.delaySeconds * 500L)
                }
            }
        } finally {
            _isRunning.value = false
            _currentStrategyIndex.value = -1
        }
        
        // Sort by success percentage (descending)
        results.sortedByDescending { it.successPercentage }
    }
    
    /**
     * Stop the current testing process
     */
    fun stop() {
        _isRunning.value = false
        testJob?.cancel()
    }
    
    /**
     * Get the best strategy from results
     * 
     * @param results List of strategy results
     * @param minSuccessPercentage Minimum success percentage required (0-100)
     * @return The best strategy, or null if none meet the criteria
     */
    fun getBestStrategy(
        results: List<Strategy>,
        minSuccessPercentage: Int = 50
    ): Strategy? {
        return results
            .filter { it.successPercentage >= minSuccessPercentage }
            .maxByOrNull { it.successPercentage }
    }
    
    /**
     * Get strategies that work well (above threshold)
     * 
     * @param results List of strategy results
     * @param threshold Success percentage threshold (0-100)
     * @return List of strategies above the threshold, sorted by success percentage
     */
    fun getWorkingStrategies(
        results: List<Strategy>,
        threshold: Int = 70
    ): List<Strategy> {
        return results
            .filter { it.successPercentage >= threshold }
            .sortedByDescending { it.successPercentage }
    }
}
