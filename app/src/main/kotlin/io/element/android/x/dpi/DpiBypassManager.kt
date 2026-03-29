package io.element.android.x.dpi

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Main manager for DPI bypass functionality.
 * Controls the ByeDPI local proxy.
 */
class DpiBypassManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DpiBypassManager"
        
        const val DEFAULT_SOCKS_PORT = 1080
        
        // Default strategy - works for most cases
        const val DEFAULT_STRATEGY = "-p -r -s -f 2 -e 2"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val byeDpiProxy = ByeDpiProxy()
    private val strategyManager = DpiStrategyManager(context)
    
    private var isProxyRunning = false
    private var currentStrategy = DEFAULT_STRATEGY
    private var currentSocksPort = DEFAULT_SOCKS_PORT
    
    data class BypassStatus(
        val isEnabled: Boolean,
        val isProxyRunning: Boolean,
        val currentStrategy: String,
        val socksPort: Int,
        val lastError: String?
    )
    
    /**
     * Start DPI bypass proxy.
     * @param strategy ByeDPI command-line arguments.
     * @param socksPort Port for the SOCKS proxy (default 1080).
     */
    suspend fun start(
        strategy: String = DEFAULT_STRATEGY,
        socksPort: Int = DEFAULT_SOCKS_PORT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            currentStrategy = strategy
            currentSocksPort = socksPort
            
            Log.i(TAG, "Starting DPI bypass with strategy: $strategy, port: $socksPort")
            
            // Build ByeDPI command
            val command = buildByeDpiCommand(strategy, socksPort)
            Log.i(TAG, "ByeDPI command: $command")
            
            // Start ByeDPI proxy
            val proxyStarted = byeDpiProxy.start(command)
            if (!proxyStarted) {
                Log.e(TAG, "Failed to start ByeDPI proxy")
                return@withContext Result.failure(Exception("Failed to start ByeDPI proxy"))
            }
            
            isProxyRunning = true
            
            // Verify proxy is running
            delay(500)
            if (!byeDpiProxy.isRunning()) {
                Log.e(TAG, "Proxy started but not running")
                return@withContext Result.failure(Exception("Proxy started but not running"))
            }
            
            Log.i(TAG, "ByeDPI proxy started successfully on port $socksPort")
            
            // Save successful strategy for network
            val networkId = strategyManager.getNetworkId()
            strategyManager.saveStrategyForNetwork(networkId, extractStrategyName(strategy), strategy)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DPI bypass: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop DPI bypass proxy.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping DPI bypass...")
        
        try {
            byeDpiProxy.stop()
            isProxyRunning = false
            Log.i(TAG, "ByeDPI proxy stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy: ${e.message}")
        }
    }
    
    /**
     * Get current bypass status.
     */
    fun getStatus(): BypassStatus {
        return BypassStatus(
            isEnabled = isProxyRunning,
            isProxyRunning = isProxyRunning,
            currentStrategy = currentStrategy,
            socksPort = currentSocksPort,
            lastError = null
        )
    }
    
    /**
     * Check if proxy is currently running.
     */
    fun isRunning(): Boolean {
        return isProxyRunning && byeDpiProxy.isRunning()
    }
    
    /**
     * Get SOCKS proxy address for network configuration.
     */
    fun getProxyAddress(): String {
        return "socks5://127.0.0.1:$currentSocksPort"
    }
    
    /**
     * Test if a strategy works for the current network.
     */
    suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult {
        return strategyManager.testStrategy(strategy, domains)
    }
    
    /**
     * Find the best strategy for current network.
     */
    suspend fun autoSelectStrategy(): String? {
        val strategies = strategyManager.loadStrategies()
        if (strategies.isEmpty()) {
            Log.w(TAG, "No strategies available")
            return null
        }
        
        val domains = strategyManager.loadTestDomains()
        var bestStrategy: String? = null
        var bestSuccess = 0f
        
        for (strategy in strategies) {
            val result = testStrategy(strategy, domains)
            if (result.successPercentage > bestSuccess) {
                bestSuccess = result.successPercentage
                bestStrategy = strategy
            }
            
            if (bestSuccess >= 90f) break
        }
        
        if (bestStrategy != null) {
            Log.i(TAG, "Best strategy: $bestStrategy with ${bestSuccess.toInt()}% success")
        }
        
        return bestStrategy
    }
    
    private fun buildByeDpiCommand(strategy: String, socksPort: Int): String {
        return buildString {
            // Basic proxy settings - listen on localhost
            append("-i 127.0.0.1 -p $socksPort ")
            
            // Protocol filters - only TCP (http/https)
            append("-K h ")
            
            // Add the bypass strategy
            append(strategy)
        }
    }
    
    private fun extractStrategyName(strategy: String): String {
        // Extract a human-readable name from the strategy
        val parts = strategy.split(" ").take(4).joinToString(" ")
        return if (parts.length > 30) parts.take(27) + "..." else parts
    }
}
