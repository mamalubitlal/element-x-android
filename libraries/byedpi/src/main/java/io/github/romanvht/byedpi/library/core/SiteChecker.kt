package io.github.romanvht.byedpi.library.core

import io.github.romanvht.byedpi.library.data.SiteCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/**
 * Utility class for checking site accessibility through a proxy
 */
class SiteChecker(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 1080
) {
    
    /**
     * Check multiple sites asynchronously
     * 
     * @param sites List of sites to check
     * @param requestsCount Number of requests to make per site
     * @param requestTimeout Timeout for each request in seconds
     * @param concurrentRequests Maximum number of concurrent requests
     * @param onProgress Callback for progress updates (site, successCount, totalRequests)
     * @return List of pairs (site, successCount)
     */
    suspend fun checkSitesAsync(
        sites: List<String>,
        requestsCount: Int,
        requestTimeout: Long,
        concurrentRequests: Int = 20,
        onProgress: ((String, Int, Int) -> Unit)? = null
    ): List<SiteCheckResult> {
        val semaphore = Semaphore(concurrentRequests)
        return withContext(Dispatchers.IO) {
            sites.map { site ->
                async {
                    semaphore.withPermit {
                        val successCount = checkSiteAccess(site, requestsCount, requestTimeout)
                        onProgress?.invoke(site, successCount, requestsCount)
                        SiteCheckResult(site, successCount, requestsCount)
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * Check a single site's accessibility
     * 
     * @param site The site URL or domain to check
     * @param requestsCount Number of requests to make
     * @param timeout Timeout for each request in seconds
     * @return Number of successful responses
     */
    suspend fun checkSiteAccess(
        site: String,
        requestsCount: Int,
        timeout: Long
    ): Int = withContext(Dispatchers.IO) {
        var responseCount = 0

        val formattedUrl = if (site.startsWith("http://") || site.startsWith("https://")) {
            site
        } else {
            "https://$site"
        }

        val url = try {
            URL(formattedUrl)
        } catch (e: Exception) {
            return@withContext 0
        }

        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyHost, proxyPort))

        repeat(requestsCount) { attempt ->
            var connection: HttpURLConnection? = null
            try {
                connection = url.openConnection(proxy) as HttpURLConnection
                connection.connectTimeout = (timeout * 1000).toInt()
                connection.readTimeout = (timeout * 1000).toInt()
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Connection", "close")

                val responseCode = connection.responseCode
                val declaredLength = connection.contentLengthLong
                
                var actualLength = 0L
                try {
                    val inputStream = if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    
                    if (inputStream != null) {
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        val limit = if (declaredLength > 0) declaredLength else 1024L * 1024

                        while (actualLength < limit) {
                            val remaining = limit - actualLength
                            val toRead = if (remaining > buffer.size) buffer.size else remaining.toInt()
                            bytesRead = inputStream.read(buffer, 0, toRead)
                            if (bytesRead == -1) break
                            actualLength += bytesRead
                        }
                    }
                } catch (_: IOException) {
                    // Stream reading failed
                }

                if (declaredLength <= 0L || actualLength >= declaredLength) {
                    responseCount++
                }
            } catch (e: Exception) {
                // Connection failed
            } finally {
                connection?.disconnect()
            }
        }

        responseCount
    }
    
    /**
     * Test proxy connectivity
     * 
     * @param timeout Timeout for the test in seconds
     * @return True if proxy is reachable
     */
    suspend fun testProxyConnection(timeout: Long = 5): Boolean = withContext(Dispatchers.IO) {
        try {
            val testUrl = URL("https://www.google.com")
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyHost, proxyPort))
            val connection = testUrl.openConnection(proxy) as HttpURLConnection
            connection.connectTimeout = (timeout * 1000).toInt()
            connection.readTimeout = (timeout * 1000).toInt()
            connection.requestMethod = "HEAD"
            
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
