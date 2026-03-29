/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.element.android.libraries.dpi.api.DpiBypassManager
import io.element.android.libraries.dpi.api.DpiStrategyManager
import io.element.android.libraries.dpi.impl.DpiBypassManagerImpl
import io.element.android.libraries.dpi.impl.DpiStrategyManagerImpl
import io.element.android.libraries.di.annotations.ApplicationContext

@BindingContainer
@ContributesTo(AppScope::class)
object DpiModule {
    @Provides
    fun provideDpiBypassManager(
        @ApplicationContext context: Context
    ): DpiBypassManager = DpiBypassManagerImpl(context)

    @Provides
    fun provideDpiStrategyManager(
        @ApplicationContext context: Context
    ): DpiStrategyManager = DpiStrategyManagerImpl(context)
}
