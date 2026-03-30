package io.element.android.x.dpi

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

data class DpiTestResult(
    val strategy: String,
    val domain: String,
    val success: Boolean,
    val responseTimeMs: Long
)

data class StrategyTestResult(
    val strategy: String,
    val command: String,
    val totalTests: Int,
    val successfulTests: Int,
    val successPercentage: Float,
    val domains: Map<String, DomainResult>
)

data class DomainResult(
    val domain: String,
    val totalTests: Int,
    val successfulTests: Int,
    val successPercentage: Float
)

data class NetworkStrategy(
    val networkId: String,
    val networkType: String,
    val bestStrategy: String,
    val bestCommand: String,
    val lastTested: Long
)

class DpiStrategyManager(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("dpi_strategies", Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    companion object {
        private const val KEY_NETWORK_STRATEGIES = "network_strategies"
        private const val KEY_LAST_TEST = "last_test_timestamp"
        private const val STRATEGY_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val SOCKS_PORT = 1080
    }
    
    @Suppress("DEPRECATION")
    fun getNetworkId(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "unknown"
        
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                "wifi_${wifiInfo.ssid ?: "unknown"}"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile_cellular"
            else -> "unknown"
        }
    }
    
    fun getStrategyForNetwork(networkId: String): NetworkStrategy? {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null) ?: return null
        val type = object : TypeToken<Map<String, NetworkStrategy>>() {}.type
        val strategies: Map<String, NetworkStrategy> = gson.fromJson(strategiesJson, type)
        return strategies[networkId]
    }
    
    fun saveStrategyForNetwork(networkId: String, strategy: String, command: String) {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null)
        val type = object : TypeToken<MutableMap<String, NetworkStrategy>>() {}.type
        val strategies: MutableMap<String, NetworkStrategy> = if (strategiesJson != null) {
            gson.fromJson(strategiesJson, type)
        } else {
            mutableMapOf()
        }
        
        strategies[networkId] = NetworkStrategy(
            networkId = networkId,
            networkType = if (networkId.startsWith("wifi")) "WiFi" else "Mobile",
            bestStrategy = strategy,
            bestCommand = command,
            lastTested = System.currentTimeMillis()
        )
        
        prefs.edit {
            putString(KEY_NETWORK_STRATEGIES, gson.toJson(strategies))
            putLong(KEY_LAST_TEST, System.currentTimeMillis())
        }
    }
    
    fun isStrategyExpired(networkId: String): Boolean {
        val strategy = getStrategyForNetwork(networkId) ?: return true
        val elapsed = System.currentTimeMillis() - strategy.lastTested
        return elapsed > STRATEGY_EXPIRY_MS
    }
    
    fun loadStrategies(): List<String> {
        return try {
            val inputStream = context.assets.open("proxytest_strategies.list")
            inputStream.bufferedReader().readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun loadTestDomains(): List<String> {
        return try {
            val inputStream = context.assets.open("proxytest_matrix.sites")
            inputStream.bufferedReader().readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
        } catch (e: Exception) {
            listOf("matrix.org", "matrix-client.matrix.org", "vector.im", "accounts.matrix.org", "turn.matrix.org")
        }
    }
    
    suspend fun testStrategy(command: String, domains: List<String>): StrategyTestResult = withContext(Dispatchers.IO) {
        val domainResults = mutableMapOf<String, DomainResult>()
        var totalSuccess = 0
        var totalTests = 0
        
        for (domain in domains) {
            var domainSuccess = 0
            val testsPerDomain = 3
            
            for (i in 1..testsPerDomain) {
                totalTests++
                val success = testDomainThroughProxy(command, domain)
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
            strategy = extractStrategyName(command),
            command = command,
            totalTests = totalTests,
            successfulTests = totalSuccess,
            successPercentage = (totalSuccess.toFloat() / totalTests) * 100,
            domains = domainResults
        )
    }
    
    private fun testDomainThroughProxy(command: String, domain: String): Boolean {
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
        val parts = command.split(" ")
        val name = parts.take(3).joinToString(" ")
        return if (name.length > 30) name.take(27) + "..." else name
    }
    
    fun saveTestResults(results: List<StrategyTestResult>, networkId: String) {
        val file = File(context.filesDir, "dpi_test_results_$networkId.json")
        file.writeText(gson.toJson(results))
    }
    
    fun loadTestResults(networkId: String): List<StrategyTestResult>? {
        return try {
            val file = File(context.filesDir, "dpi_test_results_$networkId.json")
            if (file.exists()) {
                val type = object : TypeToken<List<StrategyTestResult>>() {}.type
                gson.fromJson(file.readText(), type)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
