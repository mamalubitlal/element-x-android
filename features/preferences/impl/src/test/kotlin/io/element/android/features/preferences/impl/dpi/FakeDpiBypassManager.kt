/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import io.element.android.libraries.dpi.api.DpiBypassManager
import kotlinx.coroutines.delay

/**
 * Fake implementation of [DpiBypassManager] for testing.
 */
class FakeDpiBypassManager : DpiBypassManager {
    var isRunningValue = false
    var isNativeAvailableValue = true
    var startCalls = mutableListOf<String>()
    var stopCalls = 0
    var lastStartCommand: String? = null
    
    private var shouldStartFail = false
    private var startFailureMessage: String? = null
    
    fun configureStartToFail(message: String = "Simulated start failure") {
        shouldStartFail = true
        startFailureMessage = message
    }
    
    fun configureStartToSucceed() {
        shouldStartFail = false
        startFailureMessage = null
    }

    override suspend fun start(strategyCommand: String): Result<Unit> {
        startCalls.add(strategyCommand)
        lastStartCommand = strategyCommand
        if (shouldStartFail) {
            return Result.failure(Exception(startFailureMessage))
        }
        isRunningValue = true
        return Result.success(Unit)
    }

    override fun stop() {
        isRunningValue = false
        stopCalls++
    }

    override fun isRunning(): Boolean = isRunningValue

    override fun isNativeAvailable(): Boolean = isNativeAvailableValue

    fun reset() {
        isRunningValue = false
        isNativeAvailableValue = true
        startCalls.clear()
        stopCalls = 0
        lastStartCommand = null
        shouldStartFail = false
        startFailureMessage = null
    }
}
