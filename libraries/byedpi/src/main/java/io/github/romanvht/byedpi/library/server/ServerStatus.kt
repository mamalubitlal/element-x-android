package io.github.romanvht.byedpi.library.server

/**
 * Current status of the proxy server
 */
enum class ServerStatus {
    /** Server is stopped */
    STOPPED,
    /** Server is starting up */
    STARTING,
    /** Server is running */
    RUNNING,
    /** Server is stopping */
    STOPPING,
    /** Server encountered an error */
    ERROR
}

/**
 * Result of a server operation
 */
sealed class ServerResult {
    data class Success(val message: String = "Operation completed successfully") : ServerResult()
    data class Error(val code: Int, val message: String) : ServerResult()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): String? = (this as? Success)?.message
    fun getErrorMessage(): String? = (this as? Error)?.message
}

/**
 * Event emitted by the server
 */
sealed class ServerEvent {
    /** Server started */
    data class Started(val config: ProxyConfig) : ServerEvent()
    
    /** Server stopped */
    data class Stopped(val reason: String = "Normal shutdown") : ServerEvent()
    
    /** Server error occurred */
    data class Error(val code: Int, val message: String) : ServerEvent()
    
    /** Connection count changed */
    data class ConnectionsChanged(val activeConnections: Int) : ServerEvent()
    
    /** Status changed */
    data class StatusChanged(val oldStatus: ServerStatus, val newStatus: ServerStatus) : ServerEvent()
}

/**
 * Callback interface for server events
 */
interface ServerEventListener {
    fun onServerStarted(config: ProxyConfig)
    fun onServerStopped(reason: String)
    fun onServerError(code: Int, message: String)
    fun onStatusChanged(oldStatus: ServerStatus, newStatus: ServerStatus)
}

/**
 * Default implementation of ServerEventListener
 */
abstract class ServerEventAdapter : ServerEventListener {
    override fun onServerStarted(config: ProxyConfig) {}
    override fun onServerStopped(reason: String) {}
    override fun onServerError(code: Int, message: String) {}
    override fun onStatusChanged(oldStatus: ServerStatus, newStatus: ServerStatus) {}
}
