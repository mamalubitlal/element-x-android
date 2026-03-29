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
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of DpiStrategyManager
 */
class DpiStrategyManagerImpl(
    private val context: Context
) : DpiStrategyManager {
    
    companion object {
        private const val TAG = "DpiStrategyManager"
        private const val SOCKS_PORT = 1080
        private const val STRATEGIES_URL = "https://raw.githubusercontent.com/romanvht/ByeByeDPI/0dd7a98b0184bad978a1b50d926603328a54b94e/app/src/main/assets/proxytest_strategies.list"
        private const val STRATEGIES_CACHE_FILE = "strategies_cache.txt"
        private const val STRATEGIES_VERSION = "bye_bye_dpi_v1"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
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
        // Try to fetch from network first
        val strategies = try {
            fetchStrategiesFromNetwork()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch strategies from network: ${e.message}")
            null
        }
        
        // If network fetch failed, try cache
        strategies ?: loadStrategiesFromCache() ?: loadStrategiesFromAssets()
    }
    
    private suspend fun fetchStrategiesFromNetwork(): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching strategies from network...")
        val url = URL(STRATEGIES_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Chator/1.0")
        
        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val strategies = connection.inputStream.bufferedReader().readText()
                    .lines()
                    .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                
                // Cache the strategies
                cacheStrategies(strategies)
                Log.d(TAG, "Fetched ${strategies.size} strategies from network")
                strategies
            } else {
                Log.w(TAG, "Failed to fetch strategies: HTTP $responseCode")
                throw Exception("HTTP $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun cacheStrategies(strategies: List<String>) {
        try {
            val cacheFile = File(context.filesDir, STRATEGIES_CACHE_FILE)
            val cached = "$STRATEGIES_VERSION\n" + strategies.joinToString("\n")
            cacheFile.writeText(cached)
            Log.d(TAG, "Cached ${strategies.size} strategies")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache strategies: ${e.message}")
        }
    }
    
    private fun loadStrategiesFromCache(): List<String>? {
        return try {
            val cacheFile = File(context.filesDir, STRATEGIES_CACHE_FILE)
            if (cacheFile.exists()) {
                val lines = cacheFile.readText().lines()
                if (lines.isNotEmpty() && lines[0] == STRATEGIES_VERSION) {
                    val strategies = lines.drop(1).filter { it.isNotBlank() }
                    Log.d(TAG, "Loaded ${strategies.size} strategies from cache")
                    strategies
                } else {
                    null
                }
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load strategies from cache: ${e.message}")
            null
        }
    }
    
    private fun loadStrategiesFromAssets(): List<String> {
        return try {
            val inputStream = context.assets.open("proxytest_strategies.list")
            inputStream.bufferedReader().readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load strategies from assets: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun loadTestDomains(): List<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("proxytest_matrix.sites")
            inputStream.bufferedReader().readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load test domains: ${e.message}")
            listOf("matrix.org", "matrix-client.matrix.org", "vector.im", "accounts.matrix.org", "turn.matrix.org")
        }
    }
    
    @Suppress("DEPRECATION")
    override suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult = withContext(Dispatchers.IO) {
        val domainResults = mutableMapOf<String, DomainResult>()
        var totalSuccess = 0
        var totalTests = 0
        
        for (domain in domains) {
            var domainSuccess = 0
            val testsPerDomain = 3
            
            for (i in 1..testsPerDomain) {
                totalTests++
                val success = testDomainThroughProxy(domain)
                if (success) {
                    domainSuccess++
                    totalSuccess++
                }
            }
            
            domainResults[domain] = DomainResult(
                domain = domain,
                totalTests = testsPerDomain,
                successfulTests = domainSuccess,
                successPercentage = (domainSuccess.toFloat() / testsPerDomain) * 100
            )
        }
        
        StrategyTestResult(
            strategy = extractStrategyName(strategy),
            command = strategy,
            totalTests = totalTests,
            successfulTests = totalSuccess,
            successPercentage = if (totalTests > 0) (totalSuccess.toFloat() / totalTests) * 100 else 0f,
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
    
    private fun testDomainThroughProxy(domain: String): Boolean {
        return try {
            val socket = Socket("127.0.0.1", SOCKS_PORT)
            socket.soTimeout = 5000
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            
            val request = buildHttpRequest(domain)
            outputStream.write(request.toByteArray())
            outputStream.flush()
            
            val response = ByteArray(4096)
            val bytesRead = inputStream.read(response)
            
            socket.close()
            bytesRead > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun buildHttpRequest(domain: String): String {
        return """
            |GET / HTTP/1.1
            |Host: $domain
            |User-Agent: Chator/1.0
            |Connection: close
            |
            |
        """.trimMargin()
    }
    
    private fun extractStrategyName(command: String): String {
        // Extract meaningful name from the command
        val trimmed = command.trim()
        // Show first 25 chars max
        return if (trimmed.length > 25) {
            trimmed.take(22) + "..."
        } else {
            trimmed
        }
    }
}
