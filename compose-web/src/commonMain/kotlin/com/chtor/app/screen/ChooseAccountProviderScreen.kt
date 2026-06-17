package com.chtor.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chtor.app.ChatorColors

// Mirrors `ChooseAccountProviderView.kt` from Chator Android.
// List of well-known Matrix homeservers + "Other" option for custom URL.
data class AccountProvider(
    val title: String,
    val url: String,
    val subtitle: String? = null,
    val isMatrixOrg: Boolean = false,
)

val DefaultAccountProviders = listOf(
    AccountProvider(
        title = "matrix.org",
        url = "https://matrix.org",
        subtitle = "Открытая федеративная сеть",
        isMatrixOrg = true
    ),
    AccountProvider(
        title = "chator.crabdance.com",
        url = "https://chator.crabdance.com",
        subtitle = "Чатор — наш собственный сервер"
    ),
    AccountProvider(
        title = "envs.net",
        url = "https://envs.net",
        subtitle = "Лёгкий сервер для сообществ"
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAccountProviderScreen(
    providers: List<AccountProvider> = DefaultAccountProviders,
    onBack: () -> Unit,
    onProviderSelected: (AccountProvider) -> Unit,
    onOther: () -> Unit,
) {
    Scaffold(
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
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ChatorColors.bluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐", fontSize = 36.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Выберите сервер",
                color = ChatorColors.bluePrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Где живут ваши сообщения",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(providers) { provider ->
                    AccountProviderRow(
                        provider = provider,
                        onClick = { onProviderSelected(provider) }
                    )
                }
                item {
                    AccountProviderOtherRow(onClick = onOther)
                }
            }
        }
    }
}

@Composable
private fun AccountProviderRow(provider: AccountProvider, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChatorColors.bluePrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = provider.title.firstOrNull()?.uppercase() ?: "?",
                    color = ChatorColors.bluePrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (provider.subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = provider.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountProviderOtherRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(56.dp))
            Text(
                text = "Другой сервер…",
                color = ChatorColors.bluePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
