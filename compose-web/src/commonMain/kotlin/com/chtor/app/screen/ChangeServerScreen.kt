package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors

// Mirrors `ChangeServerView.kt` (a subview of ChangeAccountProvider) from Chator Android.
// Form to enter a custom Matrix homeserver URL.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeServerScreen(
    initialUrl: String = "https://",
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    val isValid = url.startsWith("http://", ignoreCase = true) ||
                  url.startsWith("https://", ignoreCase = true)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(ChatorColors.bluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐", fontSize = 56.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Свой сервер",
                color = ChatorColors.bluePrimary,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Введите адрес вашего Matrix homeserver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it.trim() },
                label = { Text("Адрес сервера") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (isValid) onSubmit(url) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Пример: https://matrix.example.com",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onSubmit(url) },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Продолжить")
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
