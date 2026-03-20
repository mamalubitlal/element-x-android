/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import android.app.Application
import androidx.startup.AppInitializer
import androidx.work.Configuration as WorkConfiguration
import dev.zacsweers.metro.createGraphFactory
import io.element.android.libraries.di.DependencyInjectionGraphOwner
import io.element.android.libraries.workmanager.api.di.MetroWorkerFactory
import io.element.android.x.di.AppGraph
import io.element.android.x.info.logApplicationInfo
import io.element.android.x.initializer.CacheCleanerInitializer
import io.element.android.x.initializer.CrashInitializer
import io.element.android.x.initializer.PlatformInitializer
import java.util.Locale

class ElementXApplication : Application(), DependencyInjectionGraphOwner, WorkConfiguration.Provider {
    override val graph: AppGraph = createGraphFactory<AppGraph.Factory>().create(this)

    override val workManagerConfiguration: WorkConfiguration = WorkConfiguration.Builder()
        .setWorkerFactory(MetroWorkerFactory(graph.workerProviders))
        .build()

    override fun onCreate() {
        super.onCreate()
        
        // Force Russian locale for чатор
        setRussianLocale()
        
        AppInitializer.getInstance(this).apply {
            initializeComponent(CrashInitializer::class.java)
            initializeComponent(PlatformInitializer::class.java)
            initializeComponent(CacheCleanerInitializer::class.java)
        }

        logApplicationInfo(this)
    }
    
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(updateLocale(base))
    }
    
    /**
     * Force Russian locale for чатор app.
     * Element X has 100% Russian translation via Localazy.
     */
    private fun setRussianLocale() {
        val locale = Locale("ru", "RU")
        Locale.setDefault(locale)
    }
    
    /**
     * Update context with Russian locale.
     */
    private fun updateLocale(context: android.content.Context): android.content.Context {
        val locale = Locale("ru", "RU")
        Locale.setDefault(locale)
        
        val config = android.content.res.Configuration()
        config.setTo(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
