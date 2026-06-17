package com.chtor.app.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors
import com.chtor.app.currentTimeMillis
import com.chtor.app.timezoneOffsetMillis
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.matrix.Message
import com.chtor.app.matrix.MessageStatus
import com.chtor.app.matrix.copyToClipboard
import kotlinx.coroutines.launch

// Mirrors `MessagesView.kt` from Chator Android: top bar with back/room name/call/threads,
// timeline of bubbles (self right in Chator blue, others left in subtle gray), and a
// bottom composer (attach, text field, send).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    api: MatrixApi,
    roomId: String,
    roomName: String,
    eventId: String? = null,
    onBack: () -> Unit,
    onOpenThread: (rootEventId: String, rootBody: String) -> Unit = { _, _ -> },
    onOpenRoomThreads: () -> Unit = {}
) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var initialScrolled by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<Message?>(null) }
    var threadCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(roomId) {
        if (!api.isLoggedIn()) { loading = false; return@LaunchedEffect }
        api.roomMessages(roomId)
            .onSuccess { msgs ->
                messages.clear()
                messages.addAll(msgs)
                loading = false
            }
            .onFailure { loading = false }
        // also fetch thread summary so parent bubbles can show count chip
        api.roomThreads(roomId).onSuccess { list ->
            threadCounts = list.associate { it.rootEventId to it.count }
        }
    }

    // After messages load: jump to eventId if given, else to bottom. Once-only.
    LaunchedEffect(messages.size, eventId) {
        if (initialScrolled || messages.isEmpty()) return@LaunchedEffect
        val targetIndex = if (eventId != null) {
            messages.indexOfFirst { it.id == eventId }
        } else -1
        val idx = if (targetIndex >= 0) targetIndex else messages.lastIndex
        // Negative offset to nudge the target a bit above the top of the viewport.
        listState.scrollToItem(idx, scrollOffset = -160)
        initialScrolled = true
    }

    fun send(body: String) {
        if (body.isEmpty()) return
        val localId = "local-${messages.size}-${currentTimeMillis()}"
        val mine = api.currentUserId() ?: ""
        messages.add(Message(localId, mine, body, currentTimeMillis(), isMine = true, status = MessageStatus.Sending))
        input = ""
        val lastIndex = messages.lastIndex
        scope.launch {
            listState.animateScrollToItem(lastIndex)
            api.sendMessage(roomId, body)
                .onSuccess {
                    val idx = messages.indexOfFirst { it.id == localId }
                    if (idx >= 0) messages[idx] = messages[idx].copy(status = MessageStatus.Sent)
                }
                .onFailure { err ->
                    com.chtor.app.Log.e("chat", "send failed in $roomId: ${err.message ?: err}")
                    val idx = messages.indexOfFirst { it.id == localId }
                    if (idx >= 0) messages[idx] = messages[idx].copy(status = MessageStatus.Failed)
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ChatorColors.bluePrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) { Text(roomName.firstOrNull()?.uppercase() ?: "#", color = ChatorColors.bluePrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(10.dp))
                        Text(roomName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { /* call */ }) {
                        Text("📞", fontSize = 18.sp)
                    }
                    IconButton(onClick = onOpenRoomThreads) {
                        Text("💬", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = { Composer(input, { input = it }, onSend = { send(input.trim()) }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Загрузка…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Нет сообщений. Напишите первое!", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val count = threadCounts[msg.id]
                        MessageBubble(
                            msg = msg,
                            onLongPress = { actionsFor = msg },
                            threadCount = count,
                            onOpenThread = { onOpenThread(msg.id, msg.body) }
                        )
                    }
                }
            }
        }
    }

    if (actionsFor != null) {
        val msg = actionsFor!!
        AlertDialog(
            onDismissRequest = { actionsFor = null },
            title = { Text("Сообщение", fontSize = 16.sp) },
            text = {
                Text(
                    msg.body.take(200),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    copyToClipboard(msg.body)
                    actionsFor = null
                }) { Text("Копировать") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onOpenThread(msg.id, msg.body)
                    actionsFor = null
                }) { Text("Ответить в треде") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: Message,
    onLongPress: () -> Unit,
    threadCount: Int? = null,
    onOpenThread: (() -> Unit)? = null
) {
    val align = if (msg.isMine) Alignment.End else Alignment.Start
    val bg = if (msg.isMine) ChatorColors.bluePrimary else MaterialTheme.colorScheme.surface
    val fg = if (msg.isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column {
                if (!msg.isMine) {
                    Text(
                        msg.sender.substringAfter(':').substringBefore(':').ifEmpty { msg.sender },
                        color = ChatorColors.blueLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(text = msg.body, color = fg, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(msg.timestamp),
                        color = fg.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    if (msg.isMine) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            statusGlyph(msg.status),
                            color = if (msg.status == MessageStatus.Failed)
                                Color(0xFFFF6B6B) else fg.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        if (threadCount != null && threadCount > 0 && onOpenThread != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "💬 $threadCount ${threadPlural(threadCount)}",
                fontSize = 12.sp,
                color = ChatorColors.blueLight,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .combinedClickable(
                        onClick = onOpenThread,
                        onLongClick = onLongPress
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun threadPlural(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "ответ"
    n % 10 in 2..4 && (n % 100 !in 12..14) -> "ответа"
    else -> "ответов"
}

private fun statusGlyph(status: MessageStatus): String = when (status) {
    MessageStatus.Sending -> "…"
    MessageStatus.Sent    -> "✓"
    MessageStatus.Failed  -> "!"
}

@Composable
internal fun Composer(value: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    val maxLines = 5
    val lineCount = value.count { it == '\n' } + 1
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(onClick = { /* attach */ }) {
            Text("📎", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(max = (maxLines * 22 + 20).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text("Сообщение…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    // Cap at maxLines by counting newlines
                    val nl = new.count { it == '\n' }
                    if (nl < maxLines || new.length < value.length) onChange(new)
                    else if (nl == maxLines && new.length > value.length) onChange(new)  // allow more chars on last line
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(ChatorColors.bluePrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onSend, enabled = value.isNotBlank()) {
            Text(
                "➤",
                fontSize = 22.sp,
                color = if (value.isNotBlank()) ChatorColors.bluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    // No-op use of lineCount to avoid warning; surface it on overflow only.
    @Suppress("UNUSED_EXPRESSION") lineCount
}

private fun formatTime(ts: Long): String {
    if (ts <= 0L) return ""
    val now = currentTimeMillis() + timezoneOffsetMillis()
    val diff = now - ts
    val totalMin = diff / 60_000L
    val h = totalMin / 60L
    val m = totalMin % 60L
    return "${h}:${m.toString().padStart(2, '0')}"
}
