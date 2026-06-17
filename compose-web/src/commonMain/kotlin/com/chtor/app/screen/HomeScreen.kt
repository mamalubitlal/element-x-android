package com.chtor.app.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chtor.app.matrix.ChatRoom
import com.chtor.app.matrix.MatrixApi

@Composable
fun HomeScreen(
    api: MatrixApi,
    onOpenChat: (ChatRoom) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSpace: (spaceId: String, spaceName: String) -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home")
    }
}
