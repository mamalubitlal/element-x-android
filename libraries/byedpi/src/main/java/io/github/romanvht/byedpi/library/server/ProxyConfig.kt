package io.github.romanvht.byedpi.library.server

/**
 * Configuration for the ByeDPI proxy server
 */
data class ProxyConfig(
    /** IP address to bind to */
    val ip: String = "127.0.0.1",
    /** Port to bind to */
    val port: Int = 1080,
    /** Enable HTTP CONNECT support */
    val httpConnect: Boolean = false,
    /** Maximum number of concurrent connections */
    val maxConnections: Int = 0,
    /** Buffer size */
    val bufferSize: Int = 0,
    /** Enable UDP desync */
    val desyncUdp: Boolean = false,
    /** Number of UDP fake packets */
    val udpFakeCount: Int = 0,
    /** Hosts mode (blacklist/whitelist) */
    val hostsMode: HostsMode = HostsMode.NONE,
    /** Hosts list */
    val hosts: String? = null,
    /** Custom command line arguments */
    val customArgs: String? = null,
    /** Use custom command mode instead of UI settings */
    val useCustomCommand: Boolean = false,
    /** DNS resolver mode */
    val resolveDns: Boolean = true,
    /** IPv6 support */
    val ipv6: Boolean = true,
    /** Debug mode */
    val debug: Boolean = false
) {
    
    /**
     * Build command line arguments for ByeDPI
     */
    fun buildArgs(): Array<String> {
        if (useCustomCommand && !customArgs.isNullOrEmpty()) {
            return arrayOf("ciadpi") + customArgs.split(" ").toTypedArray()
        }
        
        val args = mutableListOf("ciadpi")
        
        // IP and port
        if (ip.isNotEmpty()) args.add("-i$ip")
        if (port != 0) args.add("-p$port")
        
        // Connection settings
        if (maxConnections != 0) args.add("-c$maxConnections")
        if (bufferSize != 0) args.add("-b$bufferSize")
        
        // HTTP CONNECT
        if (httpConnect) args.add("-G")
        
        // DNS and IPv6
        if (!resolveDns) args.add("-N")
        if (!ipv6) args.add("-6")
        
        // Debug
        if (debug) args.add("-d")
        
        // UDP
        if (desyncUdp) {
            args.add("-Ku")
            if (udpFakeCount != 0) args.add("-a$udpFakeCount")
        }
        
        // Hosts filtering
        if (!hosts.isNullOrBlank() && hostsMode != HostsMode.NONE) {
            val hostStr = ":${hosts.replace("\n", " ")}"
            when (hostsMode) {
                HostsMode.BLACKLIST -> {
                    args.add("-H$hostStr")
                    args.add("-An")
                }
                HostsMode.WHITELIST -> {
                    args.add("-Kt,h")
                    args.add("-H$hostStr")
                }
                else -> {}
            }
        }
        
        return args.toTypedArray()
    }
    
    /**
     * Get the proxy address as a string
     */
    val address: String
        get() = "$ip:$port"
    
    companion object {
        /** Default configuration */
        val DEFAULT = ProxyConfig()
        
        /** Configuration for testing */
        val TESTING = ProxyConfig(
            ip = "127.0.0.1",
            port = 1080,
            debug = true
        )
        
        /**
         * Create config from command line arguments
         */
        fun fromCommand(command: String): ProxyConfig {
            val parts = command.split(" ")
            var ip = "127.0.0.1"
            var port = 1080
            var httpConnect = false
            
            for (i in parts.indices) {
                when {
                    parts[i].startsWith("-i") -> ip = parts[i].removePrefix("-i")
                    parts[i].startsWith("-p") -> port = parts[i].removePrefix("-p").toIntOrNull() ?: 1080
                    parts[i] == "-G" || parts[i] == "--http-connect" -> httpConnect = true
                }
            }
            
            return ProxyConfig(
                ip = ip,
                port = port,
                httpConnect = httpConnect,
                customArgs = command,
                useCustomCommand = true
            )
        }
    }
}

/**
 * Hosts filtering mode
 */
enum class HostsMode {
    NONE,
    BLACKLIST,
    WHITELIST
}
