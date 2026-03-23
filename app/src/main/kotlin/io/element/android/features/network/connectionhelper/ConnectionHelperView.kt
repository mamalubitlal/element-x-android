/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.network.connectionhelper

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.ui.strings.CommonStrings

data class ConnectionSolution(
    val name: String,
    val description: String,
    val type: SolutionType,
    val url: String,
    val icon: IconSource
)

enum class SolutionType {
    DPI_BYPASS,
    VPN_PAID,
    VPN_FREE
}

@Composable
internal fun ConnectionHelperView(
    onSolutionClick: (ConnectionSolution) -> Unit = {}
) {
    val context = LocalContext.current
    val solutions = getRecommendedSolutions(context)
    
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "🔧 Connection Helper",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Having trouble connecting? Try these solutions:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        items(solutions) { solution ->
            SolutionCard(
                solution = solution,
                onClick = { onSolutionClick(solution) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        item {
            Text(
                text = "💡 Tip: Start with ByeByeDPI for DPI bypass. If that doesn't work, try a VPN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun SolutionCard(
    solution: ConnectionSolution,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                iconSource = solution.icon,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = solution.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = solution.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when (solution.type) {
                        SolutionType.DPI_BYPASS -> "Free - Best for Russia"
                        SolutionType.VPN_PAID -> "Paid - More reliable"
                        SolutionType.VPN_FREE -> "Free - Limited"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                iconSource = IconSource.Vector(Icons.Default.VpnKey),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getRecommendedSolutions(context: Context): List<ConnectionSolution> {
    return listOf(
        ConnectionSolution(
            name = "ByeByeDPI",
            description = "Bypass DPI inspection without VPN. Lightweight and free.",
            type = SolutionType.DPI_BYPASS,
            url = "https://github.com/romanvht/ByeByeDPI",
            icon = IconSource.Vector(Icons.Default.Speed)
        ),
        ConnectionSolution(
            name = "ZoogVPN",
            description = "Reliable VPN with servers in multiple countries.",
            type = SolutionType.VPN_PAID,
            url = "https://zoogvpn.com/",
            icon = IconSource.Vector(Icons.Default.Security)
        ),
        ConnectionSolution(
            name = "Огонь VPN",
            description = "Russian VPN service, good for bypassing blocks.",
            type = SolutionType.VPN_PAID,
            url = "https://firevpn.ru/",
            icon = IconSource.Vector(Icons.Default.Security)
        ),
        ConnectionSolution(
            name = "ProtonVPN Free",
            description = "Free tier available, unlimited data.",
            type = SolutionType.VPN_FREE,
            url = "https://protonvpn.com/",
            icon = IconSource.Vector(Icons.Default.Security)
        ),
        ConnectionSolution(
            name = "Windscribe Free",
            description = "10GB/month free, good speeds.",
            type = SolutionType.VPN_FREE,
            url = "https://windscribe.com/",
            icon = IconSource.Vector(Icons.Default.Security)
        )
    )
}

fun openSolutionUrl(context: Context, solution: ConnectionSolution) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(solution.url))
    context.startActivity(intent)
}
