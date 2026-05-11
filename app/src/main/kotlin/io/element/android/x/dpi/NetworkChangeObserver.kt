package io.element.android.x.dpi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class NetworkEvent {
    data class Connected(val networkId: String, val networkType: NetworkType) : NetworkEvent()
    data object Disconnected : NetworkEvent()
}

enum class NetworkType {
    WIFI,
    MOBILE,
    UNKNOWN
}

class NetworkChangeObserver(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkIdCache = mutableMapOf<String, Long>()
    private val networkIdValidityMs = 60_000L // 1 minute cache

    val networkEvents: Flow<NetworkEvent> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkEvent.Disconnected) // Will be followed by Connected
            }

            override fun onLost(network: Network) {
                trySend(NetworkEvent.Disconnected)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val networkId = getNetworkIdCached()
                val networkType = getNetworkTypeFromCapabilities(networkCapabilities)
                trySend(NetworkEvent.Connected(networkId, networkType))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun getNetworkIdCached(): String {
        val now = System.currentTimeMillis()
        val cached = networkIdCache["current"]
        if (cached != null && now - cached < networkIdValidityMs) {
            return "cached_network"
        }

        val networkId = getNetworkIdSync()
        networkIdCache["current"] = now
        return networkId
    }

    @Suppress("DEPRECATION")
    private fun getNetworkIdSync(): String {
        val network = connectivityManager.activeNetwork ?: return "unknown"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                "wifi_${wifiInfo.ssid ?: "unknown"}"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile_cellular"
            else -> "unknown"
        }
    }

    private fun getNetworkType(network: Network): NetworkType {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN
        return getNetworkTypeFromCapabilities(capabilities)
    }

    private fun getNetworkTypeFromCapabilities(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.UNKNOWN
        }
    }

    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
