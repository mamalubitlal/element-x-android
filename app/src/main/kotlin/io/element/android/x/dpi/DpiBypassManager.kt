package io.element.android.x.dpi

import android.content.Context
import android.util.Log
import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.server.ProxyConfig
import io.github.romanvht.byedpi.library.server.ServerStatus

/**
 * Singleton holder for ByeDpiLibrary to avoid multiple instances.
 */
object ByeDpiLibraryHolder {
    val library: ByeDpiLibrary by lazy {
        ByeDpiLibrary()
    }
}

/**
 * Main manager for DPI bypass functionality using ByeByeDPI library.
 */
class DpiBypassManager(private val context: Context) {

    companion object {
        private const val TAG = "DpiBypassManager"
        const val DEFAULT_SOCKS_PORT = 1080
        const val DEFAULT_STRATEGY = "-p -r -s"
    }

    private val library = ByeDpiLibraryHolder.library
    private var currentStrategy = DEFAULT_STRATEGY
    private var currentSocksPort = DEFAULT_SOCKS_PORT
    private var isProxyRunning = false

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
    ): Result<Unit> {
        try {
            if (!library.isNativeLibraryAvailable()) {
                Log.e(TAG, "Native library not available")
                return Result.failure(Exception("Native library not available"))
            }

            currentStrategy = strategy
            currentSocksPort = socksPort

            Log.i(TAG, "Starting DPI bypass with strategy: $strategy, port: $socksPort")

            // Stop any running server first
            if (library.isServerRunning) {
                library.stopServer()
            }

            // Create config with strategy
            val config = ProxyConfig(
                ip = "127.0.0.1",
                port = socksPort,
                httpConnect = true,
                customArgs = strategy
            )

            val result = library.startServer(config)

            when (library.serverStatus) {
                ServerStatus.RUNNING -> {
                    isProxyRunning = true
                    Log.i(TAG, "DPI proxy started successfully on port $socksPort")
                    Result.success(Unit)
                }
                ServerStatus.ERROR -> {
                    Log.e(TAG, "Failed to start DPI proxy")
                    Result.failure(Exception("Failed to start proxy"))
                }
                else -> {
                    Log.e(TAG, "Unexpected server status: ${library.serverStatus}")
                    Result.failure(Exception("Unexpected server status: ${library.serverStatus}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DPI bypass: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Stop DPI bypass proxy.
     */
    suspend fun stop() {
        Log.i(TAG, "Stopping DPI bypass...")
        try {
            library.stopServer()
            isProxyRunning = false
            Log.i(TAG, "DPI proxy stopped")
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
            isProxyRunning = library.isServerRunning,
            currentStrategy = currentStrategy,
            socksPort = currentSocksPort,
            lastError = null
        )
    }

    /**
     * Check if proxy is currently running.
     */
    fun isRunning(): Boolean {
        return library.isServerRunning
    }

    /**
     * Get SOCKS proxy address for network configuration.
     */
    fun getProxyAddress(): String {
        return "socks5://127.0.0.1:$currentSocksPort"
    }
}