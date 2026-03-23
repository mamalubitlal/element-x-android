package io.element.android.x.dpi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

class NetworkChangeObserver(context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val dpiStrategyManager = DpiStrategyManager(context)
    
    val networkEvents: Flow<NetworkEvent> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val networkId = dpiStrategyManager.getNetworkId()
                val networkType = getNetworkType(network)
                trySend(NetworkEvent.Connected(networkId, networkType))
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkEvent.Disconnected)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val networkId = dpiStrategyManager.getNetworkId()
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
