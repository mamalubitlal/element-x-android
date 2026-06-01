package io.element.android.x

import io.element.android.appconfig.AuthenticationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pings candidate homeservers every launch, picks fastest available.
 * Falls back to default if none reachable. Prefers current URL if still competitive.
 */
object HomeserverResolver {
    private val tag = "HomeserverResolver"

    /**
     * Check all candidates on every launch. Selection is cached in prefs,
     * but next launch re-evaluates (network may have changed).
     */
    suspend fun resolve() = withContext(Dispatchers.IO) {
        val currentUrl = AuthenticationConfig.MATRIX_ORG_URL

        val results = AuthenticationConfig.CANDIDATE_HOMESERVERS.map { url ->
            async { ping(url) }
        }.awaitAll()

        val available = results.filterNotNull().sortedBy { it.latencyMs }

        if (available.isEmpty()) {
            if (currentUrl != AuthenticationConfig.DEFAULT_MATRIX_URL) {
                Timber.tag(tag).w("No candidate reachable, reset to default")
                AuthenticationConfig.resetMatrixUrl()
            }
            return@withContext
        }

        val best = available.first()

        // If current URL still works and isn't the worst option, keep it (avoid flip-flop)
        val currentStillWorks = available.any { it.url == currentUrl }
        val isLastPlace = available.lastOrNull()?.url == currentUrl
        if (currentStillWorks && !isLastPlace) {
            Timber.tag(tag).d("Current URL $currentUrl still competitive, keep it")
            return@withContext
        }

        // Switch to fastest available
        if (best.url != currentUrl) {
            Timber.tag(tag).d("Switch: $currentUrl → ${best.url} (${best.latencyMs}ms)")
            AuthenticationConfig.setCustomMatrixUrl(best.url)
        }
    }

    private data class PingResult(val url: String, val latencyMs: Long)

    private fun ping(homeserverUrl: String): PingResult? {
        val versionUrl = "$homeserverUrl/_matrix/client/versions"
        return try {
            val start = System.currentTimeMillis()
            val conn = URL(versionUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            conn.disconnect()

            if (code in 200..499) { // Any server response means it's a Matrix server
                Timber.tag(tag).d("$homeserverUrl reachable in ${latency}ms (HTTP $code)")
                PingResult(homeserverUrl, latency)
            } else {
                Timber.tag(tag).w("$homeserverUrl returned HTTP $code")
                null
            }
        } catch (e: Exception) {
            Timber.tag(tag).w(e, "$homeserverUrl unreachable")
            null
        }
    }
}
