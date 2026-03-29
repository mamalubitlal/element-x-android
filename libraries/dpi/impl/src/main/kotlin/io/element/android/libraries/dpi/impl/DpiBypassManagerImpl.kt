/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl

import android.content.Context
import android.util.Log
import io.element.android.libraries.dpi.api.DpiBypassManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of DpiBypassManager
 * Uses native ByeDPI and hev-socks5-tunnel libraries
 */
class DpiBypassManagerImpl(
    private val context: Context
) : DpiBypassManager {
    
    companion object {
        private const val TAG = "DpiBypassManager"
        const val DEFAULT_SOCKS_PORT = 1080
        const val DEFAULT_STRATEGY = "-p -r -s"
        
        private var isLibraryLoaded = false
        private var libraryLoadError: String? = null
        
        init {
            loadNativeLibraries()
        }
        
        private fun loadNativeLibraries() {
            try {
                Log.d(TAG, "Loading native libraries...")
                System.loadLibrary("byedpi")
                Log.d(TAG, "byedpi loaded")
                System.loadLibrary("hev-socks5-tunnel")
                Log.d(TAG, "hev-socks5-tunnel loaded")
                isLibraryLoaded = true
                Log.i(TAG, "Native libraries loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                libraryLoadError = e.message ?: "Failed to load native libraries"
                Log.e(TAG, "Failed to load native libraries: $libraryLoadError")
            } catch (e: Exception) {
                libraryLoadError = e.message ?: "Unknown error loading libraries"
                Log.e(TAG, "Error loading native libraries: $libraryLoadError")
            }
        }
    }
    
    // Native method declarations - calling the ByeByeDPI native functions
    private external fun nativeStartProxy(args: Array<String>): Int
    private external fun nativeStopProxy(): Int
    private external fun nativeForceClose(): Int
    
    @Volatile
    private var isProxyRunning = false
    private var currentStrategy = DEFAULT_STRATEGY
    private var currentSocksPort = DEFAULT_SOCKS_PORT
    
    override suspend fun start(strategyCommand: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Check if libraries are loaded
        if (!isLibraryLoaded) {
            Log.e(TAG, "Native libraries not loaded: ${libraryLoadError}")
            return@withContext Result.failure(
                Exception("Native libraries not available: ${libraryLoadError ?: "Unknown error"}")
            )
        }
        
        try {
            currentStrategy = strategyCommand
            Log.i(TAG, "Starting DPI bypass with strategy: $strategyCommand, port: $currentSocksPort")
            
            // Build arguments for the native proxy
            val args = buildArgs(strategyCommand, currentSocksPort)
            Log.d(TAG, "Native proxy args: ${args.joinToString(" ")}")
            
            // Start the native proxy
            val result = nativeStartProxy(args)
            
            if (result != 0) {
                Log.e(TAG, "Failed to start native proxy, result: $result")
                return@withContext Result.failure(Exception("Failed to start proxy (error $result)"))
            }
            
            isProxyRunning = true
            Log.i(TAG, "Native DPI proxy started successfully on port $currentSocksPort")
            
            Result.success(Unit)
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method error: ${e.message}")
            Result.failure(Exception("Native method error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DPI bypass: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun stop() {
        Log.i(TAG, "Stopping DPI bypass...")
        
        if (!isLibraryLoaded) {
            Log.w(TAG, "Libraries not loaded, nothing to stop")
            return
        }
        
        try {
            nativeStopProxy()
            isProxyRunning = false
            Log.i(TAG, "DPI proxy stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy: ${e.message}")
            try {
                nativeForceClose()
            } catch (ignored: Exception) {}
        }
    }
    
    override fun isRunning(): Boolean {
        return isProxyRunning
    }
    
    override fun isNativeAvailable(): Boolean {
        return isLibraryLoaded
    }
    
    private fun buildArgs(strategy: String, socksPort: Int): Array<String> {
        // Build arguments similar to how ByeByeDPI does it
        return buildList {
            add("-i")
            add("127.0.0.1")
            add("-p")
            add(socksPort.toString())
            // Add the bypass strategy options
            strategy.split(" ").filter { it.isNotBlank() }.forEach { add(it) }
        }.toTypedArray()
    }
}
