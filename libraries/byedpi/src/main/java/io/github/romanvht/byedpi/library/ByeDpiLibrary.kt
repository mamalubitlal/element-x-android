package io.github.romanvht.byedpi.library

import io.github.romanvht.byedpi.library.core.SiteChecker
import io.github.romanvht.byedpi.library.core.StrategyTester
import io.github.romanvht.byedpi.library.data.*
import io.github.romanvht.byedpi.library.jni.ByeDpiJni
import io.github.romanvht.byedpi.library.server.ByeDpiServer
import io.github.romanvht.byedpi.library.server.ProxyConfig
import io.github.romanvht.byedpi.library.server.ProxyController
import io.github.romanvht.byedpi.library.server.ServerResult
import io.github.romanvht.byedpi.library.server.ServerStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entry point for the ByeDPI Library
 */
class ByeDpiLibrary(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 1080
) {
    private val strategyTester = StrategyTester(proxyHost, proxyPort)
    private val siteChecker = SiteChecker(proxyHost, proxyPort)
    private val server = ByeDpiServer()

    init {
        // Wire the native JNI controller
        if (ByeDpiJni.isAvailable()) {
            server.setProxyController(JniProxyController)
        }
    }

    /**
     * Internal object that implements ProxyController using static JNI calls
     */
    private object JniProxyController : ProxyController {
        override fun startProxy(args: Array<String>): Int {
            return ByeDpiJni.startNativeProxy(args)
        }

        override fun stopProxy(): Int {
            return ByeDpiJni.stopNativeProxy()
        }
    }

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
     */
    fun getBestStrategy(
        results: List<Strategy>,
        minSuccessPercentage: Int = 50
    ): Strategy? = strategyTester.getBestStrategy(results, minSuccessPercentage)

    /**
     * Get strategies that work well (above threshold)
     */
    fun getWorkingStrategies(
        results: List<Strategy>,
        threshold: Int = 70
    ): List<Strategy> = strategyTester.getWorkingStrategies(results, threshold)

    // ==================== Proxy Methods ====================

    /**
     * Test if the proxy is reachable
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
     */
    suspend fun startServer(config: ProxyConfig = ProxyConfig.DEFAULT): ServerResult {
        return server.start(config)
    }

    /**
     * Start the server with a specific strategy
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
     */
    suspend fun startServerExternal(
        binaryPath: String,
        config: ProxyConfig = ProxyConfig.DEFAULT
    ): ServerResult {
        return server.startExternal(binaryPath, config)
    }

    /**
     * Stop the ByeDPI proxy server
     */
    suspend fun stopServer(): ServerResult {
        return server.stop()
    }

    /**
     * Restart the server
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
     * Whether the native library is available on this device
     */
    fun isNativeLibraryAvailable(): Boolean = ByeDpiJni.isAvailable()

    // ==================== Utility Methods ====================

    /**
     * Replace {sni} placeholder in a strategy command
     */
    fun replaceSni(command: String, sniValue: String): String =
        command.replace("{sni}", sniValue)
}

/**
 * Create a new ByeDpiLibrary instance with the specified proxy settings
 */
fun createByeDpiLibrary(proxyHost: String = "127.0.0.1", proxyPort: Int = 1080): ByeDpiLibrary =
    ByeDpiLibrary(proxyHost, proxyPort)
