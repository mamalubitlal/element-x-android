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
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.element.android.libraries.dpi.api.DomainResult
import io.element.android.libraries.dpi.api.StrategyTestResult
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

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
    
    private val gson = Gson()
    
    data class NetworkStrategy(
        val networkId: String,
        val networkType: String,
        val bestStrategy: String,
        val bestCommand: String,
        val lastTested: Long
    )
    
    fun getNetworkId(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "unknown"
        
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"
        
        return when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                "wifi_${wifiInfo.ssid ?: "unknown"}"
            }
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile_cellular"
            else -> "unknown"
        }
    }
    
    fun saveStrategyForNetwork(networkId: String, strategy: String, command: String) {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null)
        val type = object : TypeToken<MutableMap<String, NetworkStrategy>>() {}.type
        val strategies: MutableMap<String, NetworkStrategy> = if (strategiesJson != null) {
            gson.fromJson(strategiesJson, type)
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
        
        prefs.edit {
            putString(KEY_NETWORK_STRATEGIES, gson.toJson(strategies))
        }
        Log.d(TAG, "Saved strategy for network $networkId: $strategy")
    }
    
    fun getStrategyForNetwork(networkId: String): NetworkStrategy? {
        val strategiesJson = prefs.getString(KEY_NETWORK_STRATEGIES, null) ?: return null
        val type = object : TypeToken<Map<String, NetworkStrategy>>() {}.type
        val strategies: Map<String, NetworkStrategy> = gson.fromJson(strategiesJson, type)
        return strategies[networkId]
    }
}
