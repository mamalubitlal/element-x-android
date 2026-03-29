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
import io.element.android.libraries.dpi.impl.internal.ByeDpiProxy
import io.element.android.libraries.dpi.impl.internal.DpiStrategyManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Implementation of DpiBypassManager
 * Controls the ByeDPI local proxy
 */
class DpiBypassManagerImpl(
    private val context: Context
) : DpiBypassManager {
    
    companion object {
        private const val TAG = "DpiBypassManager"
        const val DEFAULT_SOCKS_PORT = 1080
        const val DEFAULT_STRATEGY = "-p -r -s -f 2 -e 2"
    }
    
    private val byeDpiProxy = ByeDpiProxy(context)
    private val strategyManager = DpiStrategyManagerHelper(context)
    
    private var isProxyRunning = false
    private var currentStrategy = DEFAULT_STRATEGY
    private var currentSocksPort = DEFAULT_SOCKS_PORT
    
    override suspend fun start(strategyCommand: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            currentStrategy = strategyCommand
            Log.i(TAG, "Starting DPI bypass with strategy: $strategyCommand, port: $currentSocksPort")
            
            // Build ByeDPI command
            val command = buildByeDpiCommand(strategyCommand, currentSocksPort)
            Log.d(TAG, "ByeDPI command: $command")
            
            // Start ByeDPI proxy
            val proxyStarted = byeDpiProxy.start(command)
            if (!proxyStarted) {
                Log.e(TAG, "Failed to start ByeDPI proxy")
                return@withContext Result.failure(Exception("Failed to start ByeDPI proxy"))
            }
            
            isProxyRunning = true
            
            // Verify proxy is running
            delay(500)
            if (!byeDpiProxy.isRunning()) {
                Log.e(TAG, "Proxy started but not running")
                return@withContext Result.failure(Exception("Proxy started but not running"))
            }
            
            Log.i(TAG, "ByeDPI proxy started successfully on port $currentSocksPort")
            
            // Save successful strategy for network
            val networkId = strategyManager.getNetworkId()
            strategyManager.saveStrategyForNetwork(networkId, extractStrategyName(strategyCommand), strategyCommand)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DPI bypass: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun stop() {
        Log.i(TAG, "Stopping DPI bypass...")
        
        try {
            byeDpiProxy.stop()
            isProxyRunning = false
            Log.i(TAG, "ByeDPI proxy stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy: ${e.message}")
        }
    }
    
    override fun isRunning(): Boolean {
        return isProxyRunning && byeDpiProxy.isRunning()
    }
    
    private fun buildByeDpiCommand(strategy: String, socksPort: Int): String {
        return buildString {
            // Basic proxy settings - listen on localhost
            append("-i 127.0.0.1 -p $socksPort ")
            
            // Protocol filters - only TCP (http/https)
            append("-K h ")
            
            // Add the bypass strategy
            append(strategy)
        }
    }
    
    private fun extractStrategyName(strategy: String): String {
        // Extract a human-readable name from the strategy
        val parts = strategy.split(" ").take(4).joinToString(" ")
        return if (parts.length > 30) parts.take(27) + "..." else parts
    }
}
