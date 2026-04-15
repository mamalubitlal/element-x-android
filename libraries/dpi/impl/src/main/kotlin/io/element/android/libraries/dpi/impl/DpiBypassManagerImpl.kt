/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl

import android.content.Context
import io.element.android.libraries.dpi.api.DpiBypassManager
import io.github.romanvht.byedpi.library.ByeDpiLibrary
import io.github.romanvht.byedpi.library.server.ProxyConfig
import io.github.romanvht.byedpi.library.server.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementation of DpiBypassManager using ByeDpiLibrary
 */
class DpiBypassManagerImpl(
    private val context: Context
) : DpiBypassManager {
    
    companion object {
        private const val TAG = "DpiBypassManager"
        const val DEFAULT_SOCKS_PORT = 1080
        const val DEFAULT_STRATEGY = "-p -r -s"
    }
    
    private val library = ByeDpiLibrary()
    
    @Volatile
    private var currentStrategy = DEFAULT_STRATEGY
    
    override suspend fun start(strategyCommand: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!library.isNativeLibraryAvailable()) {
            Timber.tag(TAG).e("Native library not available")
            return@withContext Result.failure(
                Exception("Native library not available")
            )
        }
        
        try {
            // Stop any running server first
            if (library.isServerRunning) {
                library.stopServer()
            }
            
            currentStrategy = strategyCommand
            Timber.tag(TAG).i("Starting DPI bypass with strategy: $strategyCommand")
            
            // Create config with strategy as custom args
            val config = ProxyConfig(
                ip = "127.0.0.1",
                port = DEFAULT_SOCKS_PORT,
                httpConnect = true,
                customArgs = strategyCommand
            )
            
            val result = library.startServer(config)
            
            when (library.serverStatus) {
                ServerStatus.RUNNING -> {
                    Timber.tag(TAG).i("DPI proxy started successfully on port $DEFAULT_SOCKS_PORT")
                    Result.success(Unit)
                }
                ServerStatus.ERROR -> {
                    Timber.tag(TAG).e("Failed to start DPI proxy")
                    Result.failure(Exception("Failed to start proxy"))
                }
                else -> {
                    Timber.tag(TAG).e("Unexpected server status: ${library.serverStatus}")
                    Result.failure(Exception("Unexpected server status: ${library.serverStatus}"))
                }
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start DPI bypass: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun stop() {
        Timber.tag(TAG).i("Stopping DPI bypass...")
        
        try {
            runBlocking {
                library.stopServer()
            }
            Timber.tag(TAG).i("DPI proxy stopped")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error stopping proxy: ${e.message}")
        }
    }
    
    override fun isRunning(): Boolean {
        return library.isServerRunning
    }
    
    override fun isNativeAvailable(): Boolean {
        return library.isNativeLibraryAvailable()
    }
}
