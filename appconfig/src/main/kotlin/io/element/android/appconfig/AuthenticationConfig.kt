/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

import android.content.Context
import android.content.SharedPreferences

object AuthenticationConfig {
    const val DEFAULT_MATRIX_URL = "https://chator-server.onrender.com"

    @Volatile
    private var customMatrixUrl: String? = null

    @Volatile
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("chator_settings", Context.MODE_PRIVATE)
        customMatrixUrl = prefs.getString("custom_matrix_url", null)
    }

    val MATRIX_ORG_URL: String
        get() = customMatrixUrl ?: DEFAULT_MATRIX_URL

    fun setCustomMatrixUrl(url: String) {
        customMatrixUrl = url
        prefs.edit().putString("custom_matrix_url", url).apply()
    }

    fun resetMatrixUrl() {
        customMatrixUrl = null
        prefs.edit().remove("custom_matrix_url").apply()
    }

    /**
     * URL with some docs that explain what's sliding sync and how to add it to your home server.
     */
    const val SLIDING_SYNC_READ_MORE_URL = "https://github.com/matrix-org/sliding-sync/blob/main/docs/Landing.md"

    /**
     * Force a sliding sync proxy url, if not null, the proxy url in the .well-known file will be ignored.
     */
    val SLIDING_SYNC_PROXY_URL: String? = null
}
