package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.chtor.app.matrix.SpaceNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    api: MatrixApi,
    spaceId: String,
    spaceName: String,
    onBack: () -> Unit,
    onOpenRoom: (roomId: String, roomName: String) -> Unit,
    onOpenSpace: (spaceId: String, spaceName: String) -> Unit
) {
    var children by remember { mutableStateOf<List<SpaceNode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(spaceId) {
        loading = true
        api.spaceChildren(spaceId)
            .onSuccess { children = it; loading = false }
            .onFailure { error = it.message ?: "Ошибка загрузки"; loading = false }
    }

    val spaces = children.filter { it.kind == SpaceNode.Kind.SPACE }
    val rooms  = children.filter { it.kind == SpaceNode.Kind.ROOM }

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
                    TextButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 22.sp) }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Пространство", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(spaceName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                error != null -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                children.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text("В этом пространстве пока ничего нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (spaces.isNotEmpty()) {
                        item {
                            SectionHeader("Пространства (${spaces.size})")
                        }
                        items(spaces, key = { "sp:${it.id}" }) { sp ->
                            SpaceRow(node = sp, onClick = { onOpenSpace(sp.id, sp.name) })
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                    if (rooms.isNotEmpty()) {
                        item {
                            SectionHeader("Чаты (${rooms.size})")
                        }
                        items(rooms, key = { "rm:${it.id}" }) { rm ->
                            SpaceRow(node = rm, onClick = { onOpenRoom(rm.id, rm.name) })
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpaceRow(node: SpaceNode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (node.kind == SpaceNode.Kind.SPACE)
                        ChatorColors.bluePrimary.copy(alpha = 0.18f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (node.kind == SpaceNode.Kind.SPACE) "◎" else node.name.firstOrNull()?.uppercase() ?: "#",
                color = if (node.kind == SpaceNode.Kind.SPACE) ChatorColors.bluePrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(node.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (node.memberCount > 0) {
                Text(
                    "${node.memberCount} ${memberWord(node.memberCount)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            if (node.kind == SpaceNode.Kind.SPACE) "›" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp
        )
    }
}

private fun memberWord(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "участник"
    n % 10 in 2..4 && (n % 100 !in 12..14) -> "участника"
    else -> "участников"
}
