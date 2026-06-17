/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumble.appyx.core.integrationpoint.NodeActivity
import com.bumble.appyx.core.plugin.NodeReadyObserver
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.lockscreen.api.LockScreenEntryPoint
import io.element.android.features.lockscreen.api.LockScreenLockState
import io.element.android.features.lockscreen.api.LockScreenService
import io.element.android.features.lockscreen.api.handleSecureFlag
import io.element.android.libraries.architecture.appyx.DebugNavStateNodeHost
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.core.log.logger.LoggerTag
import io.element.android.libraries.designsystem.colors.chatorColorOverride
import io.element.android.libraries.designsystem.theme.ElementThemeApp
import io.element.android.libraries.designsystem.utils.snackbar.LocalSnackbarDispatcher
import io.element.android.services.analytics.compose.LocalAnalyticsService
import io.element.android.x.di.AppBindings
import io.element.android.x.intent.SafeUriHandler
import kotlinx.coroutines.launch
import com.appodeal.ads.ApdInitializationCallback
import com.appodeal.ads.ApdInitializationError
import com.appodeal.ads.Appodeal
import io.element.android.libraries.core.data.AdProviderHolder
import io.element.android.libraries.core.data.WatchedAdsStore
import io.element.android.libraries.core.data.WatchedAdsStoreHolder
import io.element.android.features.call.impl.utils.CallServiceHolder
import io.element.android.features.call.impl.utils.DefaultCurrentCallService
import io.element.android.x.RewardedAdManager
import timber.log.Timber

private val loggerTag = LoggerTag("MainActivity")

class MainActivity : NodeActivity() {
    private lateinit var mainNode: MainNode
    private lateinit var appBindings: AppBindings
    lateinit var watchedAdsStore: WatchedAdsStore
    private lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag(loggerTag.value).d("onCreate, with savedInstanceState: ${savedInstanceState != null}")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        watchedAdsStore = AndroidWatchedAdsStore(this)
        WatchedAdsStoreHolder.instance = watchedAdsStore
        rewardedAdManager = RewardedAdManager(this, watchedAdsStore)
        RewardedAdManagerHolder.instance = rewardedAdManager
        AdProviderHolder.instance = rewardedAdManager
        
        // Initialize Appodeal
        val adTypes = Appodeal.REWARDED_VIDEO
        Appodeal.initialize(
            this,
            "YOUR_APP_KEY",
            adTypes,
            object : ApdInitializationCallback {
                override fun onInitializationFinished(errors: List<ApdInitializationError>?) {
                    Timber.tag(loggerTag.value).d("Appodeal initialization finished")
                }
            }
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                (CallServiceHolder.instance as? DefaultCurrentCallService)?.callEnded?.collect {
                    if (watchedAdsStore.watchedAds > 0) {
                        watchedAdsStore.watchedAds -= 1
                    } else {
                        rewardedAdManager.showAd(isEarningReward = false) { }
                    }
                }
            }
        }

        appBindings = bindings()
        setupLockManagement(appBindings.lockScreenService(), appBindings.lockScreenEntryPoint())
        enableEdgeToEdge()
        setContent {
            MainContent(appBindings)
        }
    }

    @Composable
    private fun MainContent(appBindings: AppBindings) {
        val migrationState = appBindings.migrationEntryPoint().present()
        val colors by remember {
            appBindings.enterpriseService().semanticColorsFlow(sessionId = null)
        }.collectAsState(SemanticColorsLightDark.default)
        ElementThemeApp(
            appPreferencesStore = appBindings.preferencesStore(),
            featureFlagService = appBindings.featureFlagService(),
            compoundLight = colors.light.chatorColorOverride(),
            compoundDark = colors.dark.chatorColorOverride(),
            buildMeta = appBindings.buildMeta()
        ) {
            CompositionLocalProvider(
                LocalSnackbarDispatcher provides appBindings.snackbarDispatcher(),
                LocalUriHandler provides SafeUriHandler(this),
                LocalAnalyticsService provides appBindings.analyticsService(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElementTheme.colors.bgCanvasDefault),
                ) {
                    if (migrationState.migrationAction.isSuccess()) {
                        MainNodeHost()
                    } else {
                        appBindings.migrationEntryPoint().Render(
                            state = migrationState,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MainNodeHost() {
        // TODO this is a temporary helper to capture the nav state in a more readable format for crash reports
        // Revert to `NodeHost` once this is fixed
        DebugNavStateNodeHost(integrationPoint = appyxV1IntegrationPoint) {
            MainNode(
                it,
                plugins = listOf(
                    object : NodeReadyObserver<MainNode> {
                        override fun init(node: MainNode) {
                            Timber.tag(loggerTag.value).d("onMainNodeInit")
                            mainNode = node
                            mainNode.handleIntent(intent)
                        }
                    },
                ),
                context = applicationContext
            )
        }
    }

    private fun setupLockManagement(
        lockScreenService: LockScreenService,
        lockScreenEntryPoint: LockScreenEntryPoint
    ) {
        lockScreenService.handleSecureFlag(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lockScreenService.lockState.collect { state ->
                    if (state == LockScreenLockState.Locked) {
                        startActivity(lockScreenEntryPoint.pinUnlockIntent(this@MainActivity))
                    }
                }
            }
        }
    }

    /**
     * Called when:
     * - the launcher icon is clicked (if the app is already running);
     * - a notification is clicked.
     * - a deep link have been clicked
     * - the app is going to background (<- this is strange)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.tag(loggerTag.value).d("onNewIntent")
        // If the mainNode is not init yet, keep the intent for later.
        // It can happen when the activity is killed by the system. The methods are called in this order :
        // onCreate(savedInstanceState=true) -> onNewIntent -> onResume -> onMainNodeInit
        if (::mainNode.isInitialized) {
            mainNode.handleIntent(intent)
        } else {
            setIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(loggerTag.value).d("onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(loggerTag.value).d("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(loggerTag.value).d("onDestroy")
    }
}
