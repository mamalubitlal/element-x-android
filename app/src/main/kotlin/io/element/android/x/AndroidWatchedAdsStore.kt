/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import android.content.Context
import android.content.SharedPreferences
import io.element.android.libraries.core.data.WatchedAdsStore

class AndroidWatchedAdsStore(context: Context) : WatchedAdsStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("watched_ads_prefs", Context.MODE_PRIVATE)

    override var watchedAds: Int
        get() = prefs.getInt("watched_ads", 0)
        set(value) = prefs.edit().putInt("watched_ads", value).apply()
}
