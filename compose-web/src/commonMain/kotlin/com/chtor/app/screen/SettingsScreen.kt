package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.chtor.app.matrix.MatrixApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    api: MatrixApi,
    onBack: () -> Unit,
    onOpenBugReport: () -> Unit = {},
    onLogout: () -> Unit
) {
    val home = remember { api.homeserver() ?: "" }
    val user = remember { api.currentUserId() ?: "" }

    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Plain header — PWA has no icon artifact, so use text glyphs.
            Surface(
                color = ChatorColors.bluePrimary,
                shadowElevation = 4.dp
            ) {
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
                        "Настройки",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Profile header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ChatorColors.bluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.take(1).uppercase().ifEmpty { "?" },
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.ifEmpty { "—" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (user.isNotEmpty()) {
                        Text(
                            text = user,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (home.isNotEmpty()) {
                        Text(
                            text = home,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider()

            // --- App info ---
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Версия", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Chator Web 0.1.0", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Text("Сервер", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(home.ifEmpty { "—" }, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Divider()

            // --- Bug report action ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBugReport() }
                    .padding(16.dp)
            ) {
                Text(
                    "Сообщить о баге",
                    color = ChatorColors.bluePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider()

            // --- Logout action ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmLogout = true }
                    .padding(16.dp)
            ) {
                Text(
                    "Выйти из аккаунта",
                    color = Color(0xFFFF6B6B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти?") },
            text = { Text("Локальные данные будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Выйти", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Отмена") }
            }
        )
    }
}
