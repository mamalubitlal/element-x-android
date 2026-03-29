/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import io.element.android.libraries.dpi.api.StrategyTestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DpiSettingsState(
    val isDpiBypassEnabled: Boolean = false,
    val isProxyRunning: Boolean = false,
    val currentStrategy: String = "",
    val strategies: ImmutableList<StrategyTestResult> = persistentListOf(),
    val isTesting: Boolean = false,
    val testingProgress: Float = 0f,
    val testingStatus: String = "",
    val selectedStrategyIndex: Int = -1,
    val bestStrategy: StrategyTestResult? = null,
    val eventSink: (DpiSettingsEvents) -> Unit,
)
