package io.element.android.x.dpi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object SiteCheckUtils {
    
    private const val TIMEOUT_MS = 5000
    private const val SOCKS_HOST = "127.0.0.1"
    private const val SOCKS_PORT = 1080
    
    data class SiteCheckResult(
        val domain: String,
        val accessible: Boolean,
        val responseCode: Int?,
        val responseTimeMs: Long,
        val error: String?
    )
    
    suspend fun checkSiteThroughProxy(domain: String, socksPort: Int = SOCKS_PORT): SiteCheckResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            val url = URL("https://$domain")
            val connection = url.openConnection() as HttpsURLConnection
            
            connection.apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Chator/1.0")
                setRequestProperty("Host", domain)
                
                try {
                    connect()
                    val responseCode = responseCode
                    val elapsed = System.currentTimeMillis() - startTime
                    
                    SiteCheckResult(
                        domain = domain,
                        accessible = responseCode in 200..399,
                        responseCode = responseCode,
                        responseTimeMs = elapsed,
                        error = null
                    )
                } catch (e: Exception) {
                    SiteCheckResult(
                        domain = domain,
                        accessible = false,
                        responseCode = null,
                        responseTimeMs = System.currentTimeMillis() - startTime,
                        error = e.message
                    )
                } finally {
                    disconnect()
                }
            }
        } catch (e: Exception) {
            SiteCheckResult(
                domain = domain,
                accessible = false,
                responseCode = null,
                responseTimeMs = System.currentTimeMillis() - startTime,
                error = e.message
            )
        }
    }
    
    suspend fun checkSitesThroughProxy(domains: List<String>, socksPort: Int = SOCKS_PORT): List<SiteCheckResult> = 
        withContext(Dispatchers.IO) {
            domains.map { domain ->
                checkSiteThroughProxy(domain, socksPort)
            }
        }
    
    fun countSuccessfulResponses(results: List<SiteCheckResult>): Int {
        return results.count { it.accessible }
    }
    
    fun getSuccessRate(results: List<SiteCheckResult>): Float {
        if (results.isEmpty()) return 0f
        return (countSuccessfulResponses(results).toFloat() / results.size) * 100
    }
    
    fun getAverageResponseTime(results: List<SiteCheckResult>): Long {
        if (results.isEmpty()) return 0
        return results.map { it.responseTimeMs }.average().toLong()
    }
}
