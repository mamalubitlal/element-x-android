@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.currentTimeMillis
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.matrix.ThreadSummary
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun RoomThreadsScreen(
    api: MatrixApi,
    roomId: String,
    roomName: String,
    onBack: () -> Unit,
    onOpenThread: (rootEventId: String, rootBody: String) -> Unit
) {
    var threads by remember { mutableStateOf<List<ThreadSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        scope.launch {
            api.roomThreads(roomId)
                .onSuccess { threads = it; loading = false }
                .onFailure { error = it.message; loading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Треды", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(roomName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ Назад", fontSize = 16.sp) }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading -> CenterText("Загрузка…")
                error != null -> CenterText("Ошибка: $error")
                threads.isEmpty() -> CenterText("В этом чате пока нет тредов")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(threads, key = { it.rootEventId }) { t ->
                        ThreadRow(t) { onOpenThread(t.rootEventId, t.body) }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(t: ThreadSummary, onClick: () -> Unit) {
    val ago = remember(t.timestamp) { relativeAgo(t.timestamp) }
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    t.sender.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.sender.substringAfter(":").substringBefore("@").ifBlank { t.sender },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(ago, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    t.body.ifBlank { "(медиа)" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (t.count > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "💬 ${t.count} ${pluralReplies(t.count)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CenterText(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .align(Alignment.Center),
        contentAlignment = Alignment.Center
    ) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun relativeAgo(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = currentTimeMillis() - ts
    val min = diff / 60_000
    val hr = min / 60
    val day = hr / 24
    return when {
        min < 1 -> "сейчас"
        min < 60 -> "${min}м"
        hr < 24 -> "${hr}ч"
        day < 7 -> "${day}д"
        else -> "${day / 7}н"
    }
}

private fun pluralReplies(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "ответ"
    n % 10 in 2..4 && (n % 100 !in 12..14) -> "ответа"
    else -> "ответов"
}
