/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import android.app.Application
import androidx.compose.material3.ComposeMaterial3Flags.isAnchoredDraggableComponentsStrictOffsetCheckEnabled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.startup.AppInitializer
import androidx.work.Configuration
import dev.zacsweers.metro.createGraphFactory
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.di.DependencyInjectionGraphOwner
import io.element.android.libraries.workmanager.api.di.MetroWorkerFactory
import io.element.android.x.di.AppGraph
import io.element.android.x.info.logApplicationInfo
import io.element.android.x.initializer.CacheCleanerInitializer
import io.element.android.x.initializer.CrashInitializer
import io.element.android.x.initializer.PlatformInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class ElementXApplication : Application(), DependencyInjectionGraphOwner, Configuration.Provider {
    override val graph: AppGraph = createGraphFactory<AppGraph.Factory>().create(this)

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setWorkerFactory(MetroWorkerFactory(graph.workerProviders))
        .build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate() {
        super.onCreate()

        setRussianLocale()

        AuthenticationConfig.init(this)

        applicationScope.launch {
            HomeserverResolver.resolve()
        }

        AppInitializer.getInstance(this).apply {
            initializeComponent(CrashInitializer::class.java)
            initializeComponent(PlatformInitializer::class.java)
            initializeComponent(CacheCleanerInitializer::class.java)
        }

        logApplicationInfo(this)

        isAnchoredDraggableComponentsStrictOffsetCheckEnabled = false
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(updateLocale(base))
    }

    private fun setRussianLocale() {
        val locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()
        Locale.setDefault(locale)
    }

    private fun updateLocale(context: android.content.Context): android.content.Context {
        val locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()
        Locale.setDefault(locale)

        val config = android.content.res.Configuration()
        config.setTo(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
