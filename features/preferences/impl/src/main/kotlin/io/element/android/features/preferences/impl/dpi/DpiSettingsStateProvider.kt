/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.x.dpi.DomainResult
import io.element.android.x.dpi.StrategyTestResult
import kotlinx.collections.immutable.toImmutableList

internal class DpiSettingsStateProvider : PreviewParameterProvider<DpiSettingsState> {
    override val values: Sequence<DpiSettingsState>
        get() = sequenceOf(
            aDpiSettingsState(),
            aDpiSettingsState(isDpiBypassEnabled = true, isProxyRunning = true),
            aDpiSettingsState(isTesting = true, testingProgress = 0.5f, testingStatus = "Testing 5 of 68..."),
            aDpiSettingsState(
                strategies = aStrategyList(),
                bestStrategy = aStrategyList().first()
            ),
        )
}

internal fun aDpiSettingsState(
    isDpiBypassEnabled: Boolean = false,
    isProxyRunning: Boolean = false,
    currentStrategy: String = "",
    strategies: List<StrategyTestResult> = emptyList(),
    isTesting: Boolean = false,
    testingProgress: Float = 0f,
    testingStatus: String = "",
    selectedStrategyIndex: Int = -1,
    bestStrategy: StrategyTestResult? = null,
) = DpiSettingsState(
    isDpiBypassEnabled = isDpiBypassEnabled,
    isProxyRunning = isProxyRunning,
    currentStrategy = currentStrategy,
    strategies = strategies.toImmutableList(),
    isTesting = isTesting,
    testingProgress = testingProgress,
    testingStatus = testingStatus,
    selectedStrategyIndex = selectedStrategyIndex,
    bestStrategy = bestStrategy,
    eventSink = {},
)

internal fun aStrategyList() = listOf(
    StrategyTestResult(
        strategy = "Strategy A",
        command = "-p -r -s -f 2 -e 2",
        totalTests = 15,
        successfulTests = 14,
        successPercentage = 93.3f,
        domains = mapOf(
            "matrix.org" to DomainResult("matrix.org", 3, 3, 100f),
            "vector.im" to DomainResult("vector.im", 3, 3, 100f),
            "matrix-client.matrix.org" to DomainResult("matrix-client.matrix.org", 3, 2, 66.7f),
            "accounts.matrix.org" to DomainResult("accounts.matrix.org", 3, 3, 100f),
            "turn.matrix.org" to DomainResult("turn.matrix.org", 3, 3, 100f),
        )
    ),
    StrategyTestResult(
        strategy = "Strategy B",
        command = "-p -r -s -f 1 -e 1",
        totalTests = 15,
        successfulTests = 12,
        successPercentage = 80f,
        domains = mapOf(
            "matrix.org" to DomainResult("matrix.org", 3, 3, 100f),
            "vector.im" to DomainResult("vector.im", 3, 2, 66.7f),
            "matrix-client.matrix.org" to DomainResult("matrix-client.matrix.org", 3, 2, 66.7f),
            "accounts.matrix.org" to DomainResult("accounts.matrix.org", 3, 3, 100f),
            "turn.matrix.org" to DomainResult("turn.matrix.org", 3, 2, 66.7f),
        )
    ),
    StrategyTestResult(
        strategy = "Strategy C",
        command = "-p -r -s -f 3",
        totalTests = 15,
        successfulTests = 8,
        successPercentage = 53.3f,
        domains = mapOf(
            "matrix.org" to DomainResult("matrix.org", 3, 2, 66.7f),
            "vector.im" to DomainResult("vector.im", 3, 1, 33.3f),
            "matrix-client.matrix.org" to DomainResult("matrix-client.matrix.org", 3, 2, 66.7f),
            "accounts.matrix.org" to DomainResult("accounts.matrix.org", 3, 1, 33.3f),
            "turn.matrix.org" to DomainResult("turn.matrix.org", 3, 2, 66.7f),
        )
    ),
)
