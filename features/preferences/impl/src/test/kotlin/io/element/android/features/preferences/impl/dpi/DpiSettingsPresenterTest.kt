/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.preferences.impl.dpi

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.dpi.api.DpiBypassManager
import io.element.android.libraries.dpi.api.StrategyTestResult
import io.element.android.services.toolbox.api.strings.StringProvider
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DpiSettingsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state has correct defaults`() = runTest {
        val presenter = createDpiSettingsPresenter()
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
        
        val presenter = createDpiSettingsPresenter(
            dpiBypassManager = fakeBypassManager,
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1) // Initial state
            awaitItem().also { state ->
                assertThat(fakeBypassManager.isRunning()).isFalse()
                state.eventSink(DpiSettingsEvents.SetEnabled(true))
            }
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
        
        val presenter = createDpiSettingsPresenter(
            dpiBypassManager = fakeBypassManager
        )
        
        presenter.test {
            skipItems(1) // Initial state
            awaitItem().also { state ->
                state.eventSink(DpiSettingsEvents.SetEnabled(false))
            }
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
        
        val presenter = createDpiSettingsPresenter(
            dpiBypassManager = fakeBypassManager
        )
        
        presenter.test {
            skipItems(1) // Initial state
            awaitItem().also { state ->
                state.eventSink(DpiSettingsEvents.SetEnabled(true))
            }
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
        
        val presenter = createDpiSettingsPresenter(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1) // Initial state
            awaitItem().also { state ->
                assertThat(state.isTesting).isFalse()
                state.eventSink(DpiSettingsEvents.StartAutoTest)
            }
            // Wait for testing to start
            awaitItem().also { state ->
                assertThat(state.isTesting).isTrue()
            }
            // Wait for testing to complete
            awaitItem().also { state ->
                if (!state.isTesting) {
                    // Testing completed
                    assertThat(state.strategies).hasSize(3)
                    assertThat(state.bestStrategy).isNotNull()
                    assertThat(fakeStrategyManager.loadStrategiesCalls).isEqualTo(1)
                    assertThat(fakeStrategyManager.loadTestDomainsCalls).isEqualTo(1)
                    assertThat(fakeStrategyManager.testStrategyCalls).hasSize(3)
                }
            }
            // Final state
            awaitItem().also { state ->
                assertThat(state.isTesting).isFalse()
            }
        }
    }

    @Test
    fun `present - StartAutoTest handles empty strategies`() = runTest {
        val fakeStrategyManager = FakeDpiStrategyManager().apply {
            configureStrategiesEmpty()
        }
        
        val presenter = createDpiSettingsPresenter(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            awaitItem().also { state ->
                state.eventSink(DpiSettingsEvents.StartAutoTest)
            }
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
        
        val presenter = createDpiSettingsPresenter(
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
        
        val presenter = createDpiSettingsPresenter(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            // Wait for saved results to load
            awaitItem().also { state ->
                assertThat(state.strategies).isNotEmpty()
                assertThat(state.bestStrategy).isNotNull()
                state.eventSink(DpiSettingsEvents.ClearTestResults)
            }
            awaitItem().also { state ->
                assertThat(state.strategies).isEmpty()
                assertThat(state.bestStrategy).isNull()
                assertThat(state.selectedStrategyIndex).isEqualTo(-1)
            }
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
        
        val presenter = createDpiSettingsPresenter(
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
        
        val presenter = createDpiSettingsPresenter(
            dpiBypassManager = fakeBypassManager,
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            awaitItem().also { state ->
                state.eventSink(DpiSettingsEvents.SetEnabled(true))
            }
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
        
        val presenter = createDpiSettingsPresenter(
            strategyManager = fakeStrategyManager
        )
        
        presenter.test {
            skipItems(1)
            awaitItem().also { state ->
                state.eventSink(DpiSettingsEvents.StartAutoTest)
            }
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

    private fun createDpiSettingsPresenter(
        dpiBypassManager: FakeDpiBypassManager = FakeDpiBypassManager(),
        strategyManager: FakeDpiStrategyManager = FakeDpiStrategyManager(),
    ): DpiSettingsPresenter {
        val context = FakeContext()
        return DpiSettingsPresenter(
            context = context,
            stringProvider = FakeStringProvider(),
            dpiBypassManager = dpiBypassManager,
            strategyManager = strategyManager,
        )
    }
}

/**
 * Fake context for testing that provides in-memory SharedPreferences.
 */
class FakeContext : Context() {
    private val prefs = mutableMapOf<String, Any?>()
    
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return FakeSharedPreferences(prefs)
    }
    
    // Stub all other methods - not needed for tests
    override fun getString(resId: Int): String = "test"
    override fun getString(resId: Int, defs: Any?): String = "test"
    override fun getPackageName(): String = "test.package"
    override fun getApplicationInfo(): android.content.pm.ApplicationInfo = android.content.pm.ApplicationInfo()
    override fun getPackageResourcePath(): String = ""
    override fun getAssets(): android.content.res.AssetManager = createPackageContext("", 0)!!.createPackageContext("", 0)!!.assets
    override fun createPackageContext(packageName: String, flags: Int): Context? = null
}

/**
 * Fake SharedPreferences for testing.
 */
class FakeSharedPreferences(private val map: MutableMap<String, Any?>) : SharedPreferences {
    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
}

class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
    override fun putString(key: String, value: String?): SharedPreferences.Editor { map[key] = value; return this }
    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { map[key] = values; return this }
    override fun putInt(key: String, value: Int): SharedPreferences.Editor { map[key] = value; return this }
    override fun putLong(key: String, value: Long): SharedPreferences.Editor { map[key] = value; return this }
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor { map[key] = value; return this }
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { map[key] = value; return this }
    override fun remove(key: String): SharedPreferences.Editor { map.remove(key); return this }
    override fun clear(): SharedPreferences.Editor { map.clear(); return this }
    override fun commit(): Boolean = true
    override fun apply() {}
}

/**
 * Simple string provider for testing that returns keys as strings.
 */
class FakeStringProvider : StringProvider {
    override fun getString(resId: Int): String = "test_string_$resId"
    override fun getString(resId: Int, vararg formatArgs: Any?): String = "test_string_$resId"
    override fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any?): String = "test_string_$resId"
}
