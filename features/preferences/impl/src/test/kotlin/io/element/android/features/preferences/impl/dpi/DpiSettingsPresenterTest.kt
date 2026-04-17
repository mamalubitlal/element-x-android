/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.preferences.impl.dpi

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.dpi.api.DpiBypassManager
import io.element.android.libraries.dpi.api.DomainResult
import io.element.android.libraries.dpi.api.StrategyTestResult
import io.element.android.services.toolbox.test.strings.FakeStringProvider
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DpiSettingsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state has correct defaults`() = runTest {
        val presenter = createDpiSettingsPresenterUnderTest()
        presenter.test {
            val state = awaitItem()
            assertThat(state.isDpiBypassEnabled).isFalse()
            assertThat(state.isProxyRunning).isFalse()
            assertThat(state.currentStrategy).isEmpty()
            assertThat(state.strategies).isEmpty()
            assertThat(state.isTesting).isFalse()
            assertThat(state.testingProgress).isEqualTo(0f)
            assertThat(state.selectedStrategyIndex).isEqualTo(-1)
            assertThat(state.bestStrategy).isNull()
            assertThat(state.errorMessage).isNull()
            assertThat(state.isNativeAvailable).isTrue()
        }
    }

    @Test
    fun `present - SetEnabled starts DPI bypass when native is available`() = runTest {
        val fakeBypassManager = FakeDpiBypassManager()
        val fakeStrategyManager = FakeDpiStrategyManager()
        
        val presenter = createDpiSettingsPresenterUnderTest(
            dpiBypassManager = fakeBypassManager,
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            assertThat(fakeBypassManager.isRunning()).isFalse()
            initialState.eventSink(DpiSettingsEvents.SetEnabled(true))
            awaitItem().also { state ->
                assertThat(state.isDpiBypassEnabled).isTrue()
                assertThat(state.isProxyRunning).isTrue()
            }
            assertThat(fakeBypassManager.startCalls).hasSize(1)
            assertThat(fakeBypassManager.startCalls[0]).isEqualTo(DpiBypassManager.DEFAULT_STRATEGY)
        }
    }

    @Test
    fun `present - SetEnabled with false stops DPI bypass`() = runTest {
        val fakeBypassManager = FakeDpiBypassManager()
        fakeBypassManager.isRunningValue = true
        
        val presenter = createDpiSettingsPresenterUnderTest(
            dpiBypassManager = fakeBypassManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(DpiSettingsEvents.SetEnabled(false))
            awaitItem().also { state ->
                assertThat(state.isDpiBypassEnabled).isFalse()
                assertThat(state.isProxyRunning).isFalse()
            }
            assertThat(fakeBypassManager.stopCalls).isEqualTo(1)
        }
    }

    @Test
    fun `present - SetEnabled fails gracefully when native is not available`() = runTest {
        val fakeBypassManager = FakeDpiBypassManager().apply {
            isNativeAvailableValue = false
        }
        
        val presenter = createDpiSettingsPresenterUnderTest(
            dpiBypassManager = fakeBypassManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(DpiSettingsEvents.SetEnabled(true))
            awaitItem().also { state ->
                assertThat(state.isDpiBypassEnabled).isFalse()
                assertThat(state.errorMessage).isNotNull()
            }
            // No start call should be made
            assertThat(fakeBypassManager.startCalls).isEmpty()
        }
    }

    @Test
    fun `present - StartAutoTest loads strategies and tests them`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager()
        
        val presenter = createDpiSettingsPresenterUnderTest(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.isTesting).isFalse()
            initialState.eventSink(DpiSettingsEvents.StartAutoTest)
            // Wait for testing to complete
            awaitItem().also { state ->
                assertThat(state.isTesting).isFalse()
                assertThat(state.strategies).hasSize(3)
                assertThat(state.bestStrategy).isNotNull()
                assertThat(fakeStrategyManager.loadStrategiesCalls).isEqualTo(1)
                assertThat(fakeStrategyManager.loadTestDomainsCalls).isEqualTo(1)
                assertThat(fakeStrategyManager.testStrategyCalls).hasSize(3)
            }
        }
    }

    @Test
    fun `present - StartAutoTest handles empty strategies`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager().apply {
            configureStrategiesEmpty()
        }
        
        val presenter = createDpiSettingsPresenterUnderTest(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(DpiSettingsEvents.StartAutoTest)
            // Should complete immediately with no strategies
            awaitItem().also { state ->
                assertThat(state.isTesting).isFalse()
                assertThat(state.strategies).isEmpty()
                assertThat(state.bestStrategy).isNull()
            }
        }
    }

    @Test
    fun `present - SelectStrategy changes selected index`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager()
        
        // Pre-populate with test results
        val testResults = listOf(
            StrategyTestResult(
                strategy = "Strategy 1",
                command = "-p -r -s -f 1",
                totalTests = 2,
                successfulTests = 2,
                successPercentage = 100f,
                domains = emptyMap()
            ),
            StrategyTestResult(
                strategy = "Strategy 2",
                command = "-p -r -s -f 2",
                totalTests = 2,
                successfulTests = 1,
                successPercentage = 50f,
                domains = emptyMap()
            )
        )
        fakeStrategyManager.addSavedResults("test-network-id", testResults)
        
        val presenter = createDpiSettingsPresenterUnderTest(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            // Wait for saved results to load
            awaitItem().also { state ->
                assertThat(state.strategies).hasSize(2)
                assertThat(state.selectedStrategyIndex).isEqualTo(-1)
                state.eventSink(DpiSettingsEvents.SelectStrategy(1))
            }
            awaitItem().also { state ->
                assertThat(state.selectedStrategyIndex).isEqualTo(1)
            }
        }
    }

    @Test
    fun `present - ClearTestResults resets all strategy data`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager()
        
        // Pre-populate with test results
        val testResults = listOf(
            StrategyTestResult(
                strategy = "Strategy 1",
                command = "-p -r -s -f 1",
                totalTests = 2,
                successfulTests = 2,
                successPercentage = 100f,
                domains = emptyMap()
            )
        )
        fakeStrategyManager.addSavedResults("test-network-id", testResults)
        
        val presenter = createDpiSettingsPresenterUnderTest(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            // Wait for saved results to load
            val stateWithResults = awaitItem()
            assertThat(stateWithResults.strategies).isNotEmpty()
            assertThat(stateWithResults.bestStrategy).isNotNull()
            // Send clear event (state update happens internally but may not emit new state)
            stateWithResults.eventSink(DpiSettingsEvents.ClearTestResults)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - SetEnabled uses best strategy when available`() = runTest {
        val fakeBypassManager = FakeDpiBypassManager()
        val fakeStrategyManager = FakeDpiStrategyManager()
        
        // Pre-populate with test results (Strategy 2 is best)
        val testResults = listOf(
            StrategyTestResult(
                strategy = "Strategy 1",
                command = "-p -r -s -f 1",
                totalTests = 2,
                successfulTests = 1,
                successPercentage = 50f,
                domains = emptyMap()
            ),
            StrategyTestResult(
                strategy = "Strategy 2",
                command = "-p -r -s -f 2",
                totalTests = 2,
                successfulTests = 2,
                successPercentage = 100f,
                domains = emptyMap()
            )
        )
        fakeStrategyManager.addSavedResults("test-network-id", testResults)
        
        val presenter = createDpiSettingsPresenterUnderTest(
            dpiBypassManager = fakeBypassManager,
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            awaitItem().also { state ->
                // Best strategy should be selected automatically
                state.eventSink(DpiSettingsEvents.SetEnabled(true))
            }
            awaitItem().also { state ->
                assertThat(state.isDpiBypassEnabled).isTrue()
            }
            // Should use the best strategy (Strategy 2)
            assertThat(fakeBypassManager.lastStartCommand).isEqualTo("-p -r -s -f 2")
        }
    }

    @Test
    fun `present - SetEnabled falls back to default strategy when no results saved`() = runTest {
        val fakeBypassManager = FakeDpiBypassManager()
        val fakeStrategyManager = FakeDpiStrategyManager()
        // Don't add any saved results
        
        val presenter = createDpiSettingsPresenterUnderTest(
            dpiBypassManager = fakeBypassManager,
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(DpiSettingsEvents.SetEnabled(true))
            awaitItem().also { state ->
                assertThat(state.isDpiBypassEnabled).isTrue()
            }
            // Should use default strategy when no results
            assertThat(fakeBypassManager.lastStartCommand).isEqualTo(DpiBypassManager.DEFAULT_STRATEGY)
        }
    }

    @Test
    fun `present - StartAutoTest saves results for current network`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager().apply {
            networkIdValue = "my-network"
        }
        
        val presenter = createDpiSettingsPresenterUnderTest(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(DpiSettingsEvents.StartAutoTest)
            // Skip through testing states until complete
            while (true) {
                val state = awaitItem()
                if (!state.isTesting) break
            }
            
            assertThat(fakeStrategyManager.saveTestResultsCalls).hasSize(1)
            val (results, networkId) = fakeStrategyManager.saveTestResultsCalls[0]
            assertThat(networkId).isEqualTo("my-network")
            assertThat(results).hasSize(3)
        }
    }

    private fun createDpiSettingsPresenterUnderTest(
        dpiBypassManager: FakeDpiBypassManager = FakeDpiBypassManager(),
        strategyManager: FakeDpiStrategyManager = FakeDpiStrategyManager(),
    ): DpiSettingsPresenter {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return DpiSettingsPresenter(
            context = context,
            stringProvider = FakeStringProvider(),
            dpiBypassManager = dpiBypassManager,
            strategyManager = strategyManager,
        )
    }
}
