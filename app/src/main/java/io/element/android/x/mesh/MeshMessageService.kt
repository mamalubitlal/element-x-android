package io.element.android.x.mesh

import android.content.Context
import android.util.Log
import com.bitchat.lib.model.BitchatMessage
import com.bitchat.lib.mesh.BluetoothMeshService
import com.bitchat.lib.util.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshMessageService(context: Context) {

    companion object {
        private const val TAG = "MeshMessageService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val meshService: BluetoothMeshService

    private val _connectedPeersCount = MutableStateFlow(0)
    val connectedPeersCount: StateFlow<Int> = _connectedPeersCount

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _incomingMessages = MutableSharedFlow<BitchatMessage>()
    val incomingMessages: SharedFlow<BitchatMessage> = _incomingMessages

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    init {
        meshService = BluetoothMeshService(context)
        setupCallbacks()
    }

    private fun setupCallbacks() {
        // The BluetoothMeshService has internal callbacks
        // We'll observe peer changes through the service
    }

    fun startMesh() {
        scope.launch {
            try {
                Log.d(TAG, "Starting mesh service...")
                _isScanning.value = true
                _isActive.value = true
                // Mesh service auto-starts scanning on init
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start mesh", e)
                _isActive.value = false
            }
        }
    }

    fun stopMesh() {
        scope.launch {
            try {
                Log.d(TAG, "Stopping mesh service...")
                _isScanning.value = false
                _isActive.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop mesh", e)
            }
        }
    }

    fun sendMessage(channel: String, content: String) {
        scope.launch {
            try {
                Log.d(TAG, "Sending mesh message to $channel: $content")
                // Message sending would go through mesh service
                // For now, just log it
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    fun getMyPeerId(): String {
        return meshService.myPeerID
    }

    fun updatePeersCount(count: Int) {
        _connectedPeersCount.value = count
    }

    fun destroy() {
        stopMesh()
    }
}