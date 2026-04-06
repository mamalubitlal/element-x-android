package io.github.romanvht.byedpi.library.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Interface for controlling the native proxy.
 * Implement this in your app to bridge with native code.
 */
interface ProxyController {
    /**
     * Start the native proxy with given arguments.
     * Returns 0 on success, non-zero on failure.
     */
    fun startProxy(args: Array<String>): Int

    /**
     * Stop the native proxy.
     * Returns 0 on success, non-zero on failure.
     */
    fun stopProxy(): Int
}

/**
 * Manages the ByeDPI proxy server lifecycle.
 *
 * Uses a ProxyController to delegate actual native operations.
 * Register your controller with [setProxyController] before calling [start].
 */
class ByeDpiServer {

    private val _status = MutableStateFlow(ServerStatus.STOPPED)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow<ProxyConfig?>(null)
    val config: StateFlow<ProxyConfig?> = _config.asStateFlow()

    private var serverThread: Thread? = null
    private var process: Process? = null
    private var eventListener: ServerEventListener? = null
    private var proxyController: ProxyController? = null

    /**
     * Whether the server is currently running
     */
    val isRunning: Boolean get() = _status.value == ServerStatus.RUNNING

    /**
     * Current server status
     */
    val currentStatus: ServerStatus get() = _status.value

    /**
     * Current configuration (if running)
     */
    val currentConfig: ProxyConfig? get() = _config.value

    /**
     * Set the proxy controller that handles native operations
     */
    fun setProxyController(controller: ProxyController?) {
        proxyController = controller
    }

    /**
     * Set event listener for server events
     */
    fun setEventListener(listener: ServerEventListener?) {
        eventListener = listener
    }

    /**
     * Start the proxy server
     *
     * @param config Proxy configuration
     * @return Result of the start operation
     */
    suspend fun start(config: ProxyConfig = ProxyConfig.DEFAULT): ServerResult = withContext(Dispatchers.IO) {
        if (isRunning) {
            return@withContext ServerResult.Error(-1, "Server is already running")
        }

        val controller = proxyController
            ?: return@withContext ServerResult.Error(-1, "No ProxyController set. Call setProxyController() first.")

        updateStatus(ServerStatus.STARTING)
        _config.value = config

        try {
            val args = config.buildArgs()

            // Start server in a separate thread
            serverThread = Thread {
                var result: Int = -1
                try {
                    result = controller.startProxy(args)
                } catch (e: Exception) {
                    result = -1
                }

                if (result == 0) {
                    updateStatus(ServerStatus.RUNNING)
                    eventListener?.onServerStarted(config)
                } else {
                    updateStatus(ServerStatus.ERROR)
                    eventListener?.onServerError(result, "Failed to start proxy (code: $result)")
                }
            }

            serverThread?.name = "ByeDPI-Server"
            serverThread?.start()

            // Wait a bit for server to start
            Thread.sleep(500)

            if (_status.value == ServerStatus.STARTING) {
                updateStatus(ServerStatus.RUNNING)
            }

            ServerResult.Success("Server started on ${config.address}")
        } catch (e: Exception) {
            updateStatus(ServerStatus.ERROR)
            ServerResult.Error(-1, e.message ?: "Failed to start server")
        }
    }

    /**
     * Stop the proxy server
     *
     * @return Result of the stop operation
     */
    suspend fun stop(): ServerResult = withContext(Dispatchers.IO) {
        if (!isRunning) {
            return@withContext ServerResult.Error(-1, "Server is not running")
        }

        updateStatus(ServerStatus.STOPPING)

        try {
            // Stop native proxy
            proxyController?.stopProxy()

            // Wait for thread to finish
            serverThread?.join(5000)

            // Kill process if still running
            process?.destroy()

            updateStatus(ServerStatus.STOPPED)
            eventListener?.onServerStopped("Normal shutdown")
            _config.value = null

            ServerResult.Success("Server stopped")
        } catch (e: Exception) {
            updateStatus(ServerStatus.ERROR)
            ServerResult.Error(-1, e.message ?: "Failed to stop server")
        }
    }

    /**
     * Restart the server with new configuration
     *
     * @param config New configuration (null to use current)
     * @return Result of the restart operation
     */
    suspend fun restart(config: ProxyConfig? = null): ServerResult {
        val current = _config.value
        stop()
        return start(config ?: current ?: ProxyConfig.DEFAULT)
    }

    /**
     * Start the proxy using an external process
     * This is useful when the native library is not available
     *
     * @param binaryPath Path to the ByeDPI binary
     * @param config Proxy configuration
     * @return Result of the start operation
     */
    suspend fun startExternal(
        binaryPath: String,
        config: ProxyConfig = ProxyConfig.DEFAULT
    ): ServerResult = withContext(Dispatchers.IO) {
        if (isRunning) {
            return@withContext ServerResult.Error(-1, "Server is already running")
        }

        updateStatus(ServerStatus.STARTING)
        _config.value = config

        try {
            val args = config.buildArgs().drop(1) // Remove "ciadpi" prefix for external binary
            val command = listOf(binaryPath) + args

            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            // Read output in background
            Thread {
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Log output if needed
                    }
                } catch (e: Exception) {
                    // Stream closed
                }
            }.start()

            // Wait for server to start
            Thread.sleep(1000)

            if (process?.isAlive == true) {
                updateStatus(ServerStatus.RUNNING)
                eventListener?.onServerStarted(config)
                ServerResult.Success("Server started on ${config.address}")
            } else {
                updateStatus(ServerStatus.ERROR)
                val exitCode = process?.exitValue() ?: -1
                ServerResult.Error(exitCode, "Process exited with code $exitCode")
            }
        } catch (e: Exception) {
            updateStatus(ServerStatus.ERROR)
            ServerResult.Error(-1, e.message ?: "Failed to start external process")
        }
    }

    /**
     * Check if the server is responsive
     */
    fun ping(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(_config.value?.ip ?: "127.0.0.1", _config.value?.port ?: 1080), 1000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun updateStatus(newStatus: ServerStatus) {
        val oldStatus = _status.value
        _status.value = newStatus
        eventListener?.onStatusChanged(oldStatus, newStatus)
    }
}
