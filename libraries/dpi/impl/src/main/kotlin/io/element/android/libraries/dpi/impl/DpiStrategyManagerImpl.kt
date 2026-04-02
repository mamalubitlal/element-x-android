/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import io.element.android.libraries.dpi.api.DpiStrategyManager
import io.element.android.libraries.dpi.api.DomainResult
import io.element.android.libraries.dpi.api.StrategyTestResult
import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.picker.StrategyPicker
import io.github.romanvht.byedpi.library.data.Strategy
import io.github.romanvht.byedpi.library.data.TestConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of DpiStrategyManager using ByeDpiLibrary
 */
class DpiStrategyManagerImpl(
    private val context: Context
) : DpiStrategyManager {
    
    companion object {
        private const val TAG = "DpiStrategyManager"
        private const val SOCKS_PORT = 1080
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    private val library = ByeDpiLibrary()
    private val strategyPicker = StrategyPicker()
    
    @Suppress("DEPRECATION")
    override suspend fun getNetworkId(): String = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return@withContext "unknown"
        
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withContext "unknown"
        
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                "wifi_${wifiInfo.ssid ?: "unknown"}"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile_cellular"
            else -> "unknown"
        }
    }
    
    override suspend fun loadStrategies(): List<String> = withContext(Dispatchers.IO) {
        // Use ByeDpiLibrary's built-in strategies
        val strategies = library.getDefaultStrategies().map { it.command }
        Log.d(TAG, "Loaded ${strategies.size} default strategies")
        strategies
    }
    
    override suspend fun loadTestDomains(): List<String> = withContext(Dispatchers.IO) {
        // Use ByeDpiLibrary's active domains (Matrix-related sites)
        val domains = library.getActiveDomains()
        if (domains.isNotEmpty()) {
            Log.d(TAG, "Using ByeDpiLibrary active domains: ${domains.size}")
            return@withContext domains
        }
        
        // Fallback to common Matrix domains
        Log.d(TAG, "Using fallback Matrix test domains")
        listOf(
            "matrix.org",
            "matrix-client.matrix.org",
            "vector.im",
            "accounts.matrix.org",
            "turn.matrix.org",
            "synapse.matano.dev"
        )
    }
    
    override suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Testing strategy: $strategy on ${domains.size} domains")
        
        // Create a strategy object
        val strategyObj = library.createStrategy(
            command = strategy,
            name = extractStrategyName(strategy),
            description = "Tested strategy"
        )
        
        // Test the strategy using ByeDpiLibrary
        val result = library.testSingleStrategy(
            strategy = strategyObj,
            sites = domains,
            requestsPerSite = 2,
            timeoutSeconds = 5
        )
        
        // Convert to our format
        val domainResults = mutableMapOf<String, DomainResult>()
        result.siteResults.forEach { siteResult ->
            domainResults[siteResult.site] = DomainResult(
                domain = siteResult.site,
                totalTests = siteResult.totalCount,
                successfulTests = siteResult.successCount,
                successPercentage = siteResult.successPercentage.toFloat()
            )
        }
        
        StrategyTestResult(
            strategy = result.name,
            command = result.command,
            totalTests = result.totalRequests,
            successfulTests = result.successCount,
            successPercentage = result.successPercentage.toFloat(),
            domains = domainResults
        )
    }
    
    override suspend fun loadTestResults(networkId: String): List<StrategyTestResult>? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "dpi_test_results_$networkId.json")
            if (file.exists()) {
                json.decodeFromString<List<StrategyTestResult>>(file.readText())
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load test results: ${e.message}")
            null
        }
    }
    
    override suspend fun saveTestResults(results: List<StrategyTestResult>, networkId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "dpi_test_results_$networkId.json")
            file.writeText(json.encodeToString(results))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save test results: ${e.message}")
        }
    }
    
    private fun extractStrategyName(command: String): String {
        val trimmed = command.trim()
        return if (trimmed.length > 25) {
            trimmed.take(22) + "..."
        } else {
            trimmed
        }
    }
}
