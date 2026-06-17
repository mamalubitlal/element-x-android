package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors

// Mirrors `ConfirmAccountProviderView.kt` from Chator Android.
// Big "X" icon, server title in blue, subtitle, "Continue" + "Change" buttons.
@Composable
fun ConfirmAccountProviderScreen(
    provider: AccountProvider,
    isAccountCreation: Boolean = false,
    onContinue: () -> Unit,
    onChange: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(ChatorColors.bluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 56.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isAccountCreation)
                    "Создать аккаунт на ${provider.title}"
                else
                    "Войти в ${provider.title}",
                color = ChatorColors.bluePrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isAccountCreation)
                    "Регистрация проходит на сервере ${provider.url}"
                else
                    "Все ваши сообщения будут храниться на ${provider.url}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            // Buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Продолжить")
                    }
                    TextButton(
                        onClick = onChange,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Изменить сервер")
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
