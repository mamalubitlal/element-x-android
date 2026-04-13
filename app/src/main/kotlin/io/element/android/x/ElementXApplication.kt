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
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import timber.log.Timber
import java.net.URL
import java.util.Locale
import io.element.android.libraries.core.log.logger.LoggerTag

class ElementXApplication : Application(), DependencyInjectionGraphOwner, WorkConfiguration.Provider {
    companion object {
        private val loggerTag = LoggerTag("ElementXApplication")
    }

    override val graph: AppGraph = createGraphFactory<AppGraph.Factory>().create(this)

    override val workManagerConfiguration: WorkConfiguration = WorkConfiguration.Builder()
        .setWorkerFactory(MetroWorkerFactory(graph.workerProviders))
        .build()

    override fun onCreate() {
        super.onCreate()
        
        // Force Russian locale for чатор
        setRussianLocale()
        
        // Initialize Jitsi SDK for embedded group calls
        initializeJitsiMeet()

        AppInitializer.getInstance(this).apply {
            initializeComponent(CrashInitializer::class.java)
            initializeComponent(PlatformInitializer::class.java)
            initializeComponent(CacheCleanerInitializer::class.java)
        }

        logApplicationInfo(this)
    }

    /**
     * Initialize Jitsi Meet SDK with default conference options.
     */
    private fun initializeJitsiMeet() {
        try {
            val defaultOptions = JitsiMeetConferenceOptions.Builder()
                .setServerURL(URL("https://meet.jit.si"))
                .setFeatureFlag("add-people.enabled", false)
                .setFeatureFlag("calendar.enabled", false)
                .setFeatureFlag("call-integration.enabled", false)
                .setFeatureFlag("close-captions.enabled", false)
                .setFeatureFlag("chat.enabled", true)
                .setFeatureFlag("invite.enabled", false)
                .setFeatureFlag("live-streaming.enabled", false)
                .setFeatureFlag("meeting-name.enabled", false)
                .setFeatureFlag("meeting-password.enabled", false)
                .setFeatureFlag("pip.enabled", true)
                .setFeatureFlag("prejoinpage.enabled", false)
                .setFeatureFlag("recording.enabled", false)
                .setFeatureFlag("tile-view.enabled", true)
                .setFeatureFlag("welcomepage.enabled", false)
                .build()
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
        } catch (e: Exception) {
            // Jitsi SDK not available, will use browser fallback
            Timber.tag(loggerTag.value).w(e, "Jitsi SDK initialization failed, using browser fallback")
        }
    }
    
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(updateLocale(base))
    }
    
    /**
     * Force Russian locale for чатор app.
     * Element X has 100% Russian translation via Localazy.
     */
    private fun setRussianLocale() {
        val locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()
        Locale.setDefault(locale)
    }
    
    /**
     * Update context with Russian locale.
     */
    private fun updateLocale(context: android.content.Context): android.content.Context {
        val locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()
        Locale.setDefault(locale)
        
        val config = android.content.res.Configuration()
        config.setTo(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
