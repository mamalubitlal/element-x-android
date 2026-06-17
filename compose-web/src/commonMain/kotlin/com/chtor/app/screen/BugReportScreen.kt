@file:OptIn(ExperimentalMaterial3Api::class)

package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.Log
import com.chtor.app.formatLogReport
import com.chtor.app.matrix.MatrixApi
import com.chtor.app.matrix.copyToClipboard
import com.chtor.app.matrix.platformUserAgent

@Composable
fun BugReportScreen(
    api: MatrixApi,
    onBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }
    var includeEnv  by remember { mutableStateOf(true) }
    var reportText by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщить о баге", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ Назад", fontSize = 16.sp) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Опишите проблему как можно подробнее. Отчёт собирается " +
                "на этом устройстве — отправьте его разработчикам (в чат, на почту, в GitHub) " +
                "вручную, через кнопку «Копировать».",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                label = { Text("Что произошло?") },
                placeholder = { Text("Шаги для воспроизведения, ожидаемое поведение, фактическое…") }
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = includeLogs, onCheckedChange = { includeLogs = it })
                Spacer(Modifier.width(8.dp))
                Text("Приложить логи (последние ${Log.tail().size} записей)", fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = includeEnv, onCheckedChange = { includeEnv = it })
                Spacer(Modifier.width(8.dp))
                Text("Приложить окружение (user agent, homeserver)", fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    reportText = buildReport(api, description, includeLogs, includeEnv)
                    copied = false
                },
                enabled = description.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сформировать отчёт") }

            if (reportText != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Отчёт готов", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        reportText!!,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        copyToClipboard(reportText!!)
                        copied = true
                    }) { Text(if (copied) "Скопировано ✓" else "Копировать") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { reportText = null }) { Text("Закрыть") }
                }
            }
        }
    }
}

private fun buildReport(
    api: MatrixApi,
    description: String,
    includeLogs: Boolean,
    includeEnv: Boolean
): String {
    val sb = StringBuilder()
    sb.append("== Chator Bug Report ==\n")
    sb.append("App: Chator Web (Compose/Wasm)\n")
    if (includeEnv) {
        sb.append("User agent: ").append(platformUserAgent()).append('\n')
        runCatching { sb.append("Homeserver: ").append(api.homeserver().orEmpty()).append('\n') }
        runCatching { sb.append("User ID:    ").append(api.currentUserId().orEmpty()).append('\n') }
    }
    sb.append("Timestamp:  ").append(com.chtor.app.currentTimeMillis()).append(" ms\n")
    sb.append('\n')
    sb.append("== Description ==\n")
    sb.append(description.trim()).append("\n\n")
    if (includeLogs) {
        sb.append("== Logs ==\n")
        sb.append(formatLogReport(Log.tail()))
    }
    return sb.toString()
}
