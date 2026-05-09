package io.element.android.x.features.mesh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.element.android.x.mesh.MeshMessageService
import io.element.android.x.mesh.NetworkConnectivityManager

data class MeshState(
    val isOnline: Boolean = true,
    val isMeshActive: Boolean = false,
    val connectedPeers: Int = 0,
    val myPeerId: String = "",
    val messages: List<MeshMessage> = emptyList(),
)

data class MeshMessage(
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Composable
fun rememberMeshState(): MeshState {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) }
    var isMeshActive by remember { mutableStateOf(false) }
    var connectedPeers by remember { mutableStateOf(0) }
    var messages by remember { mutableStateOf<List<MeshMessage>>(emptyList()) }
    var myPeerId by remember { mutableStateOf("") }

    val connectivityManager = remember { NetworkConnectivityManager(context) }
    val meshService = remember { MeshMessageService(context) }

    // Observe network state
    LaunchedEffect(Unit) {
        connectivityManager.isOnline.collect { online ->
            isOnline = online
            if (online) {
                meshService.stopMesh()
                isMeshActive = false
            } else {
                meshService.startMesh()
                isMeshActive = true
            }
        }
    }

    // Observe mesh state
    LaunchedEffect(Unit) {
        meshService.connectedPeersCount.collect { count ->
            connectedPeers = count
        }
    }

    LaunchedEffect(Unit) {
        myPeerId = meshService.getMyPeerId()
    }

    DisposableEffect(Unit) {
        onDispose {
            meshService.destroy()
        }
    }

    return MeshState(
        isOnline = isOnline,
        isMeshActive = isMeshActive,
        connectedPeers = connectedPeers,
        myPeerId = myPeerId,
        messages = messages,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshScreen(
    state: MeshState,
    onSendMessage: (String) -> Unit,
) {
    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status banner when offline
        if (!state.isOnline) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9800)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No internet - Using mesh network",
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Connection info card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your ID: ${state.myPeerId.take(8)}...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected peers: ${state.connectedPeers}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.isMeshActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: Mesh active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Messages list
        if (state.messages.isEmpty()) {
            Text(
                text = if (state.isOnline) "Messages via Matrix server" else "Waiting for mesh messages...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.messages) { message ->
                    MeshMessageItem(message)
                }
            }
        }

        // Message input when offline
        if (!state.isOnline) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSendMessage(messageInput)
                            messageInput = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@Composable
private fun MeshMessageItem(message: MeshMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.senderId.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}