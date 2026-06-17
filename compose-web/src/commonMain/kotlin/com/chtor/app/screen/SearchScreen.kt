package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.matrix.SearchHit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    api: MatrixApi,
    onBack: () -> Unit,
    onOpenHit: (roomId: String, roomName: String, eventId: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    // Debounced search — fire 300ms after typing stops.
    LaunchedEffect(query) {
        job?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            hits = emptyList(); error = null; loading = false
            return@LaunchedEffect
        }
        job = scope.launch {
            delay(300)
            loading = true; error = null
            api.searchAllMessages(q)
                .onSuccess { hits = it; loading = false }
                .onFailure { error = it.message ?: "Ошибка поиска"; loading = false }
        }
    }

    Scaffold(
        topBar = {
            Surface(color = ChatorColors.bluePrimary, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text("←", color = Color.White, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Поиск",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Input row
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Сообщение или чат…") },
                singleLine = true,
                leadingIcon = { Text("🔍", fontSize = 18.sp) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { query = "" }) { Text("×") }
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    query.trim().length < 2 -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Введите минимум 2 символа",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    hits.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Ничего не найдено",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(hits, key = { "${it.roomId}|${it.eventId}" }) { hit ->
                                SearchHitRow(hit = hit, query = query, onClick = {
                                    onOpenHit(hit.roomId, hit.roomName, hit.eventId)
                                })
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hit.roomName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = ChatorColors.bluePrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "@${hit.sender.substringAfter(":")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = highlight(hit.body, query),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3
            )
        }
    }
}

private fun highlight(body: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(body)
    val q = query.trim()
    return buildAnnotatedString {
        var i = 0
        val lower = body.lowercase()
        val ql = q.lowercase()
        while (i < body.length) {
            val idx = lower.indexOf(ql, i)
            if (idx < 0) {
                append(body.substring(i))
                break
            }
            if (idx > i) append(body.substring(i, idx))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, background = Color(0x33FFEB3B))) {
                append(body.substring(idx, idx + ql.length))
            }
            i = idx + ql.length
        }
    }
}
