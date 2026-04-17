/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import io.element.android.libraries.dpi.api.DomainResult
import io.element.android.libraries.dpi.api.DpiStrategyManager
import io.element.android.libraries.dpi.api.StrategyTestResult
import kotlinx.coroutines.delay

/**
 * Fake implementation of [DpiStrategyManager] for testing.
 */
class FakeDpiStrategyManager : DpiStrategyManager {
    var networkIdValue = "test-network-id"
    var strategiesValue = listOf("-p -r -s -f 1", "-p -r -s -f 2", "-p -r -s -f 3")
    var testDomainsValue = listOf("example.com", "test.org")
    
    private var savedResults = mutableMapOf<String, List<StrategyTestResult>>()
    var loadStrategiesCalls = 0
    var loadTestDomainsCalls = 0
    var testStrategyCalls = mutableListOf<Pair<String, List<String>>>()
    var loadTestResultsCalls = mutableListOf<String>()
    var saveTestResultsCalls = mutableListOf<Pair<List<StrategyTestResult>, String>>()
    
    private var shouldStrategiesBeEmpty = false
    private var shouldTestFail = false
    
    fun configureStrategiesEmpty() {
        shouldStrategiesBeEmpty = true
    }
    
    fun configureStrategiesAvailable() {
        shouldStrategiesBeEmpty = false
    }
    
    fun configureTestToFail() {
        shouldTestFail = true
    }
    
    fun configureTestToSucceed() {
        shouldTestFail = false
    }
    
    fun addSavedResults(networkId: String, results: List<StrategyTestResult>) {
        savedResults[networkId] = results
    }

    override suspend fun getNetworkId(): String {
        return networkIdValue
    }

    override suspend fun loadStrategies(): List<String> {
        loadStrategiesCalls++
        return if (shouldStrategiesBeEmpty) emptyList() else strategiesValue
    }

    override suspend fun loadTestDomains(): List<String> {
        loadTestDomainsCalls++
        return testDomainsValue
    }

    override suspend fun testStrategy(strategy: String, domains: List<String>): StrategyTestResult {
        testStrategyCalls.add(strategy to domains)
        
        val domainResults = domains.associateWith { domain ->
            if (shouldTestFail) {
                DomainResult(domain = domain, totalTests = 1, successfulTests = 0, successPercentage = 0f)
            } else {
                DomainResult(domain = domain, totalTests = 1, successfulTests = 1, successPercentage = 100f)
            }
        }
        
        val totalTests = domains.size
        val successfulTests = if (shouldTestFail) 0 else domains.size
        
        return StrategyTestResult(
            strategy = strategy,
            command = strategy,
            totalTests = totalTests,
            successfulTests = successfulTests,
            successPercentage = if (totalTests > 0) (successfulTests.toFloat() / totalTests) * 100 else 0f,
            domains = domainResults
        )
    }

    override suspend fun loadTestResults(networkId: String): List<StrategyTestResult>? {
        loadTestResultsCalls.add(networkId)
        return savedResults[networkId]
    }

    override suspend fun saveTestResults(results: List<StrategyTestResult>, networkId: String) {
        saveTestResultsCalls.add(results to networkId)
        savedResults[networkId] = results
    }

    fun reset() {
        networkIdValue = "test-network-id"
        strategiesValue = listOf("-p -r -s -f 1", "-p -r -s -f 2", "-p -r -s -f 3")
        testDomainsValue = listOf("example.com", "test.org")
        savedResults.clear()
        loadStrategiesCalls = 0
        loadTestDomainsCalls = 0
        testStrategyCalls.clear()
        loadTestResultsCalls.clear()
        saveTestResultsCalls.clear()
        shouldStrategiesBeEmpty = false
        shouldTestFail = false
    }
}
