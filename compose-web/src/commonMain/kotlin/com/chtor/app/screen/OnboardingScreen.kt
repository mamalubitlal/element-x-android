package com.chtor.app.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors
import com.chtor.app.currentTimeMillis
import com.chtor.app.timezoneOffsetMillis
import chator_web.generated.resources.Res
import chator_web.generated.resources.onboarding_bg
import chator_web.generated.resources.onboarding_bg_light
import chator_web.generated.resources.onboarding_logo
import org.jetbrains.compose.resources.painterResource

// Mirrors `OnBoardingView.kt` from the Chator Android app.
// Background: trademark Chator cyan→blue radial gradient image (light/dark variants).
// Layout: systemBarsPadding, top-spacer, centered logo, blue "Чатoр" title, secondary
// welcome text, 2 buttons (Войти, Создать аккаунт) + version footer.
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onCreateAccount: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Trademark Chator gradient — full-bleed, cropped to fill.
        Image(
            painter = painterResource(
                if (isDark) Res.drawable.onboarding_bg else Res.drawable.onboarding_bg_light
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            // Logo block — mirrors OnBoardingLogo when onBoardingLogoResId != null.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.onboarding_logo),
                    contentDescription = "Chator logo",
                    modifier = Modifier.height(100.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Чатор",
                    color = ChatorColors.bluePrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Безопасный мессенджер для ваших разговоров",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Buttons + version footer — mirrors OnBoardingButtons column.
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Войти") }

                TextButton(
                    onClick = onCreateAccount,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Создать аккаунт") }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Регистрация проходит на выбранном сервере — после создания аккаунта вы сразу войдёте в чат.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "v1.0.0 · ${currentYear()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun currentYear(): String {
    val now = currentTimeMillis() + timezoneOffsetMillis()
    val days = now / 86_400_000L
    val z = (days / 365.25).toInt()
    val y = 1970 + z
    return y.toString()
}
