/*
 * Copyright (c) 2025 чатор
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.x.developer

import android.content.Context
import android.widget.Toast
import io.element.android.appconfig.AuthenticationConfig

/**
 * Developer settings for чатор.
 * Access by tapping app name 5 times.
 */
object DeveloperSettings {
    
    private var tapCount = 0
    private var lastTapTime = 0L
    private const val TAP_LIMIT = 5
    private const val TAP_TIMEOUT = 2000L // 2 seconds
    
    /**
     * Call this when user taps on app name.
     * Returns true if developer settings should be shown.
     */
    fun onAppNameTap(context: Context): Boolean {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastTapTime > TAP_TIMEOUT) {
            tapCount = 0
        }
        
        tapCount++
        lastTapTime = currentTime
        
        if (tapCount >= TAP_LIMIT) {
            tapCount = 0
            showDeveloperSettings(context)
            return true
        }
        
        // Show toast when close to unlocking
        if (tapCount >= 3) {
            Toast.makeText(context, "Ещё ${TAP_LIMIT - tapCount} раз(а)", Toast.LENGTH_SHORT).show()
        }
        
        return false
    }
    
    private fun showDeveloperSettings(context: Context) {
        Toast.makeText(context, "⚙️ Настройки разработчика", Toast.LENGTH_LONG).show()
        // In a full implementation, this would open a settings activity
        // For now, we just notify the user
    }
    
    /**
     * Change the default Matrix server URL.
     */
    fun setServerUrl(url: String) {
        AuthenticationConfig.setCustomMatrixUrl(url)
    }
    
    /**
     * Reset to default server (chator.k.vu).
     */
    fun resetServerUrl() {
        AuthenticationConfig.resetMatrixUrl()
    }
    
    /**
     * Get current server URL.
     */
    fun getServerUrl(): String {
        return AuthenticationConfig.MATRIX_ORG_URL
    }
}
