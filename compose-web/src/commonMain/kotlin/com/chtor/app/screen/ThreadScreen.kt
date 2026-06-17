package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors
import com.chtor.app.currentTimeMillis
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.matrix.Message
import com.chtor.app.matrix.MessageStatus
import com.chtor.app.matrix.copyToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(
    api: MatrixApi,
    roomId: String,
    roomName: String,
    rootEventId: String,
    rootBody: String,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(rootEventId) {
        api.threadMessages(roomId, rootEventId)
            .onSuccess { messages = it; loading = false }
            .onFailure { loading = false }
    }

    fun send(body: String) {
        if (body.isEmpty()) return
        val localId = "local-${messages.size}-${currentTimeMillis()}"
        val mine = api.currentUserId() ?: ""
        messages = messages + Message(localId, mine, body, currentTimeMillis(), isMine = true, status = MessageStatus.Sending)
        input = ""
        scope.launch {
            api.sendThreadMessage(roomId, rootEventId, body)
                .onSuccess {
                    messages = messages.map { if (it.id == localId) it.copy(status = MessageStatus.Sent) else it }
                }
                .onFailure {
                    messages = messages.map { if (it.id == localId) it.copy(status = MessageStatus.Failed) else it }
                }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Surface(color = ChatorColors.bluePrimary, shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 22.sp) }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Тред", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(roomName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        },
        bottomBar = { Composer(input, { input = it }, onSend = { send(input.trim()) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Parent context block
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Ответ на:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        rootBody,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Загрузка…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    messages.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text("Нет ответов. Будьте первым!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ThreadMessageBubble(msg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadMessageBubble(msg: Message) {
    val align = if (msg.isMine) Alignment.End else Alignment.Start
    val bg = if (msg.isMine) ChatorColors.bluePrimary else MaterialTheme.colorScheme.surface
    val fg = if (msg.isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(msg.body, color = fg, fontSize = 15.sp)
        }
        if (msg.isMine) {
            Text(
                when (msg.status) {
                    MessageStatus.Sending -> "…"
                    MessageStatus.Sent -> "✓"
                    MessageStatus.Failed -> "!"
                },
                fontSize = 10.sp,
                color = if (msg.status == MessageStatus.Failed) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
            )
        }
    }
}
