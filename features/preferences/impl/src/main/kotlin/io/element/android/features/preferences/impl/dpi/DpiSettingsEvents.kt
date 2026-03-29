/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import io.element.android.libraries.dpi.api.StrategyTestResult

sealed interface DpiSettingsEvents {
    data class SetEnabled(val enabled: Boolean) : DpiSettingsEvents
    data object StartAutoTest : DpiSettingsEvents
    data object StopAutoTest : DpiSettingsEvents
    data class SelectStrategy(val index: Int) : DpiSettingsEvents
    data object ClearTestResults : DpiSettingsEvents
    data class TestStrategy(val result: StrategyTestResult) : DpiSettingsEvents
}
