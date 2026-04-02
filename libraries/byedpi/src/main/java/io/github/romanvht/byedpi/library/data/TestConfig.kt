package io.github.romanvht.byedpi.library.data

/**
 * Configuration for testing strategies
 */
data class TestConfig(
    /** Delay between starting each strategy (in seconds) */
    val delaySeconds: Int = 1,
    /** Number of requests to make per site */
    val requestsPerSite: Int = 1,
    /** Timeout for each request (in seconds) */
    val requestTimeoutSeconds: Long = 5,
    /** Maximum number of concurrent requests */
    val maxConcurrentRequests: Int = 20,
    /** SNI value to use in strategies that require it */
    val sniValue: String = "google.com",
    /** Whether to use full logging during testing */
    val fullLog: Boolean = false
) {
    companion object {
        /** Default configuration */
        val DEFAULT = TestConfig()
        
        /** Quick test configuration (fewer requests, shorter timeout) */
        val QUICK = TestConfig(
            requestsPerSite = 1,
            requestTimeoutSeconds = 3,
            maxConcurrentRequests = 30
        )
        
        /** Thorough test configuration (more requests, longer timeout) */
        val THOROUGH = TestConfig(
            requestsPerSite = 3,
            requestTimeoutSeconds = 10,
            maxConcurrentRequests = 10
        )
    }
}
