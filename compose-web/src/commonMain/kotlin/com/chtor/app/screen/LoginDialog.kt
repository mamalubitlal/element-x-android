package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors

// Reusable login dialog: homeserver URL, username, password. Submits via [onSubmit].
// The hosting composable drives the actual API call.
@Composable
fun LoginDialog(
    initialHomeserver: String,
    onDismiss: () -> Unit,
    onSubmit: (homeserver: String, username: String, password: String) -> Unit
) {
    var homeserver by remember { mutableStateOf(initialHomeserver) }
    var username   by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var showPwd    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Войти в Чатор", color = ChatorColors.bluePrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Введите данные вашего аккаунта", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = homeserver,
                    onValueChange = { homeserver = it },
                    label = { Text("Сервер") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    label = { Text("Имя пользователя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPwd = !showPwd }) { Text(if (showPwd) "Скрыть" else "Показать") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(homeserver.trim(), username, password) },
                enabled = homeserver.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            ) { Text("Войти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
