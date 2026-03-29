/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl.internal

import android.content.Context
import android.util.Log

/**
 * Stub implementation of ByeDpiProxy for the library module.
 * The actual implementation with native code is in the app module.
 */
class ByeDpiProxy(
    @Suppress("UNUSED_PARAMETER") context: Context
) {
    companion object {
        private const val TAG = "ByeDpiProxy"
    }
    
    init {
        // Native library is loaded in the app module
    }
    
    fun start(command: String): Boolean {
        // This is a stub - actual implementation is in app module
        Log.d(TAG, "Stub start called with: $command")
        // In the app module, this would call nativeStart()
        // For now, return true to allow testing UI to work
        return true
    }
    
    fun stop(): Boolean {
        Log.d(TAG, "Stub stop called")
        // In the app module, this would call nativeStop()
        return true
    }
    
    fun isRunning(): Boolean {
        // In the app module, this would call nativeIsRunning()
        return false
    }
}
