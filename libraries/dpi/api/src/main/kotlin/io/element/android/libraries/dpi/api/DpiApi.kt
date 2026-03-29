/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.api

/**
 * Data class representing the result of testing a DPI bypass strategy
 */
data class StrategyTestResult(
    val strategy: String,
    val command: String,
    val totalTests: Int,
    val successfulTests: Int,
    val successPercentage: Float,
    val domains: Map<String, DomainResult>
)

/**
 * Data class representing the result of testing a single domain
 */
data class DomainResult(
    val domain: String,
    val totalTests: Int,
    val successfulTests: Int,
    val successPercentage: Float
)

/**
 * Interface for DPI bypass management
 */
interface DpiBypassManager {
    /**
     * Start the DPI bypass proxy with the given strategy command
     */
    suspend fun start(strategyCommand: String): Result<Unit>
    
    /**
     * Stop the DPI bypass proxy
     */
    fun stop()
    
    /**
     * Check if the proxy is currently running
     */
    fun isRunning(): Boolean
    
    companion object {
        const val DEFAULT_STRATEGY = "-p -r -s -f 2"
    }
}

/**
 * Interface for DPI strategy testing
 */
interface DpiStrategyManager {
    /**
     * Get the current network identifier
     */
    suspend fun getNetworkId(): String
    
    /**
     * Load all available strategies from assets
     */
    suspend fun loadStrategies(): List<String>
    
    /**
     * Load test domains from assets
     */
    suspend fun loadTestDomains(): List<String>
    
    /**
     * Test a single strategy against the given domains
     */
    suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult
    
    /**
     * Load saved test results for a network
     */
    suspend fun loadTestResults(networkId: String): List<StrategyTestResult>?
    
    /**
     * Save test results for a network
     */
    suspend fun saveTestResults(results: List<StrategyTestResult>, networkId: String)
}
