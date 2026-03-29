/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dev.zacsweers.metro.Inject
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.dpi.api.DpiBypassManager
import io.element.android.libraries.dpi.api.DpiStrategyManager
import io.element.android.libraries.dpi.api.StrategyTestResult
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.services.toolbox.api.strings.StringProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Inject
class DpiSettingsPresenter(
    @ApplicationContext private val context: Context,
    private val stringProvider: StringProvider,
    private val dpiBypassManager: DpiBypassManager,
    private val strategyManager: DpiStrategyManager,
) : Presenter<DpiSettingsState> {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("dpi_settings", Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val KEY_DPI_ENABLED = "dpi_bypass_enabled"
        private const val KEY_STRATEGY_INDEX = "selected_strategy_index"
    }
    
    @Composable
    override fun present(): DpiSettingsState {
        val coroutineScope = rememberCoroutineScope()
        
        // Check if native libraries are available
        val isNativeAvailable = dpiBypassManager.isNativeAvailable()
        
        // Create StateFlows to track preference changes
        val dpiEnabledFlow = remember { MutableStateFlow(prefs.getBoolean(KEY_DPI_ENABLED, false)) }
        val strategyIndexFlow = remember { MutableStateFlow(prefs.getInt(KEY_STRATEGY_INDEX, -1)) }
        
        val isDpiBypassEnabled by dpiEnabledFlow.collectAsState()
        val selectedStrategyIndex by strategyIndexFlow.collectAsState()
        
        // Error message state
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        // Strategy testing state
        var isTesting by remember { mutableStateOf(false) }
        var testingProgress by remember { mutableStateOf(0f) }
        var testingStatus by remember { mutableStateOf("") }
        var strategies by remember { mutableStateOf<List<StrategyTestResult>>(emptyList()) }
        var bestStrategy by remember { mutableStateOf<StrategyTestResult?>(null) }
        
        // Load saved strategies for current network
        LaunchedEffect(Unit) {
            val networkId = strategyManager.getNetworkId()
            val savedResults = strategyManager.loadTestResults(networkId)
            if (savedResults != null) {
                strategies = savedResults
                // Find best strategy
                bestStrategy = savedResults.maxByOrNull { it.successPercentage }
            }
        }
        
        // Proxy running state
        var isProxyRunning by remember { mutableStateOf(dpiBypassManager.isRunning()) }
        var currentStrategy by remember { mutableStateOf("") }
        
        fun handleEvent(event: DpiSettingsEvents) {
            when (event) {
                is DpiSettingsEvents.SetEnabled -> {
                    if (!isNativeAvailable) {
                        errorMessage = stringProvider.getString(R.string.screen_dpi_error_native_not_available)
                        return
                    }
                    
                    coroutineScope.launch {
                        if (event.enabled) {
                            // Find best or selected strategy
                            val currentIndex = strategyIndexFlow.first()
                            val strategyToUse = if (currentIndex >= 0 && currentIndex < strategies.size) {
                                strategies[currentIndex].command
                            } else {
                                bestStrategy?.command ?: DpiBypassManager.DEFAULT_STRATEGY
                            }
                            
                            val result = dpiBypassManager.start(strategyToUse)
                            if (result.isSuccess) {
                                isProxyRunning = true
                                currentStrategy = strategyToUse
                                prefs.edit { putBoolean(KEY_DPI_ENABLED, true) }
                                dpiEnabledFlow.value = true
                                errorMessage = null
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: stringProvider.getString(R.string.screen_dpi_error_start_failed)
                            }
                        } else {
                            dpiBypassManager.stop()
                            isProxyRunning = false
                            currentStrategy = ""
                            prefs.edit { putBoolean(KEY_DPI_ENABLED, false) }
                            dpiEnabledFlow.value = false
                        }
                    }
                }
                
                is DpiSettingsEvents.StartAutoTest -> {
                    coroutineScope.launch {
                        isTesting = true
                        testingProgress = 0f
                        testingStatus = stringProvider.getString(R.string.screen_dpi_test_status_loading_strategies)
                        
                        val allStrategies = strategyManager.loadStrategies()
                        val testDomains = strategyManager.loadTestDomains()
                        
                        if (allStrategies.isEmpty()) {
                            testingStatus = stringProvider.getString(R.string.screen_dpi_test_error_no_strategies)
                            isTesting = false
                            return@launch
                        }
                        
                        val testResults = mutableListOf<StrategyTestResult>()
                        val totalStrategies = allStrategies.size
                        
                        for ((index, strategy) in allStrategies.withIndex()) {
                            testingStatus = stringProvider.getString(
                                R.string.screen_dpi_test_status_testing,
                                index + 1,
                                totalStrategies
                            )
                            testingProgress = (index.toFloat() / totalStrategies)
                            
                            // Test this strategy
                            val result = strategyManager.testStrategy(strategy, testDomains)
                            testResults.add(result)
                            
                            // Update progress
                            testingProgress = ((index + 1).toFloat() / totalStrategies)
                        }
                        
                        strategies = testResults.toImmutableList()
                        bestStrategy = testResults.maxByOrNull { it.successPercentage }
                        
                        // Save results for this network
                        val networkId = strategyManager.getNetworkId()
                        strategyManager.saveTestResults(testResults, networkId)
                        
                        testingStatus = stringProvider.getString(R.string.screen_dpi_test_complete)
                        isTesting = false
                    }
                }
                
                is DpiSettingsEvents.StopAutoTest -> {
                    // For now, just mark as not testing - full cancellation would require Job management
                    isTesting = false
                    testingStatus = ""
                }
                
                is DpiSettingsEvents.SelectStrategy -> {
                    strategyIndexFlow.value = event.index
                    prefs.edit { putInt(KEY_STRATEGY_INDEX, event.index) }
                    
                    // If currently running, restart with new strategy
                    if (isProxyRunning && event.index >= 0 && event.index < strategies.size) {
                        coroutineScope.launch {
                            dpiBypassManager.stop()
                            val newStrategy = strategies[event.index].command
                            val result = dpiBypassManager.start(newStrategy)
                            if (result.isSuccess) {
                                currentStrategy = newStrategy
                            }
                        }
                    }
                }
                
                is DpiSettingsEvents.ClearTestResults -> {
                    strategies = emptyList()
                    bestStrategy = null
                    strategyIndexFlow.value = -1
                    prefs.edit { putInt(KEY_STRATEGY_INDEX, -1) }
                }
                
                is DpiSettingsEvents.TestStrategy -> {
                    // Individual strategy test result - update in list
                    val index = strategies.indexOfFirst { it.command == event.result.command }
                    if (index >= 0) {
                        val newList = strategies.toMutableList()
                        newList[index] = event.result
                        strategies = newList
                        
                        // Update best if this is better
                        val currentBest = bestStrategy
                        if (currentBest == null || event.result.successPercentage > currentBest.successPercentage) {
                            bestStrategy = event.result
                        }
                    }
                }
            }
        }
        
        return DpiSettingsState(
            isDpiBypassEnabled = isDpiBypassEnabled,
            isProxyRunning = isProxyRunning,
            currentStrategy = currentStrategy,
            strategies = strategies.toImmutableList(),
            isTesting = isTesting,
            testingProgress = testingProgress,
            testingStatus = testingStatus,
            selectedStrategyIndex = selectedStrategyIndex,
            bestStrategy = bestStrategy,
            errorMessage = errorMessage,
            isNativeAvailable = isNativeAvailable,
            eventSink = ::handleEvent,
        )
    }
}
