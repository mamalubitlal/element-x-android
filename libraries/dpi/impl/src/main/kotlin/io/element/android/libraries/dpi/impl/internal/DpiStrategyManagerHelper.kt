/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dpi.impl.internal

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Internal helper for strategy management
 */
class DpiStrategyManagerHelper(
    private val context: Context
) {
    companion object {
        private const val TAG = "DpiStrategyManagerHelper"
        private const val KEY_NETWORK_STRATEGIES = "network_strategies"
        private const val SOCKS_PORT = 1080
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("dpi_strategies", Context.MODE_PRIVATE)
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Serializable
    data class NetworkStrategy(
        val networkId: String,
        val networkType: String,
        val bestStrategy: String,
        val bestCommand: String,
        val lastTested: Long
    )
    
    @Suppress("DEPRECATION")
    fun getNetworkId(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "unknown"
        
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"
        
        return when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                "wifi_${wifiInfo.ssid ?: "unknown"}"
            }
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile_cellular"
            else -> "unknown"
        }
    }
    
    fun saveStrategyForNetwork(networkId: String, strategy: String, command: String) {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null)
        val strategies: MutableMap<String, NetworkStrategy> = if (strategiesJson != null) {
            try {
                json.decodeFromString<MutableMap<String, NetworkStrategy>>(strategiesJson)
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
        
        strategies[networkId] = NetworkStrategy(
            networkId = networkId,
            networkType = if (networkId.startsWith("wifi")) "WiFi" else "Mobile",
            bestStrategy = strategy,
            bestCommand = command,
            lastTested = System.currentTimeMillis()
        )
        
        prefs.edit()
            .putString(KEY_NETWORK_STRATEGIES, json.encodeToString(strategies))
            .apply()
        Log.d(TAG, "Saved strategy for network $networkId: $strategy")
    }
    
    fun getStrategyForNetwork(networkId: String): NetworkStrategy? {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null) ?: return null
        return try {
            val strategies: Map<String, NetworkStrategy> = json.decodeFromString(strategiesJson)
            strategies[networkId]
        } catch (e: Exception) {
            null
        }
    }
}
