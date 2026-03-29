/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.dpi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.list.RadioButtonListItem
import io.element.android.libraries.designsystem.components.list.SwitchListItem
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.dpi.api.StrategyTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiSettingsView(
    state: DpiSettingsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isTesting) {
        ProgressDialog(
            text = state.testingStatus,
            type = io.element.android.libraries.designsystem.components.ProgressDialogType.Determinate(state.testingProgress),
        )
    }
    
    BackHandler {
        onBack()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            titleStr = stringResource(R.string.screen_dpi_title),
            navigationIcon = {
                BackButton(onClick = onBack)
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enable/Disable toggle
            item {
                DpiBypassToggleItem(
                    isEnabled = state.isDpiBypassEnabled,
                    isProxyRunning = state.isProxyRunning,
                    currentStrategy = state.currentStrategy,
                    onToggle = { enabled ->
                        state.eventSink(DpiSettingsEvents.SetEnabled(enabled))
                    }
                )
            }
            
            // Best strategy indicator
            state.bestStrategy?.let { best ->
                item {
                    BestStrategyCard(
                        strategy = best,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Auto-test section
            item {
                AutoTestSection(
                    isTesting = state.isTesting,
                    hasStrategies = state.strategies.isNotEmpty(),
                    onStartTest = { state.eventSink(DpiSettingsEvents.StartAutoTest) },
                    onClearResults = { state.eventSink(DpiSettingsEvents.ClearTestResults) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Strategy list
            if (state.strategies.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.screen_dpi_strategies_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                itemsIndexed(
                    items = state.strategies,
                    key = { _, result -> result.command }
                ) { index, strategy ->
                    StrategyListItem(
                        strategy = strategy,
                        isSelected = index == state.selectedStrategyIndex,
                        isBest = strategy == state.bestStrategy,
                        onSelect = { state.eventSink(DpiSettingsEvents.SelectStrategy(index)) }
                    )
                }
            }
            
            // Footer info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.screen_dpi_footer_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DpiBypassToggleItem(
    isEnabled: Boolean,
    isProxyRunning: Boolean,
    currentStrategy: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchListItem(
        modifier = modifier,
        headline = stringResource(R.string.screen_dpi_enable_title),
        supportingText = if (isProxyRunning) {
            stringResource(R.string.screen_dpi_enable_running, currentStrategy)
        } else {
            stringResource(R.string.screen_dpi_enable_description)
        },
        value = isEnabled,
        onChange = onToggle
    )
}

@Composable
private fun BestStrategyCard(
    strategy: StrategyTestResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .animateContentSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_dpi_best_strategy),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CompoundIcons.CheckCircle(),
                contentDescription = null,
                tint = MaterialTheme.colors.success,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${strategy.successPercentage.toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.success
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.screen_dpi_success_rate,
                    strategy.successfulTests,
                    strategy.totalTests
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AutoTestSection(
    isTesting: Boolean,
    hasStrategies: Boolean,
    onStartTest: () -> Unit,
    onClearResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartTest,
            enabled = !isTesting,
            modifier = Modifier.weight(1f)
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.screen_dpi_test_strategies))
        }
        
        if (hasStrategies) {
            OutlinedButton(
                onClick = onClearResults,
                enabled = !isTesting
            ) {
                Text(stringResource(R.string.screen_dpi_clear_results))
            }
        }
    }
}

@Composable
private fun StrategyListItem(
    strategy: StrategyTestResult,
    isSelected: Boolean,
    isBest: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RadioButtonListItem(
        modifier = modifier,
        headline = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strategy.strategy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isBest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.screen_dpi_best_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colors.success
                    )
                }
            }
        },
        supportingText = {
            Text(
                text = stringResource(
                    R.string.screen_dpi_strategy_success,
                    strategy.successPercentage.toInt(),
                    strategy.successfulTests,
                    strategy.totalTests
                )
            )
        },
        selected = isSelected,
        onClick = onSelect
    )
}

@PreviewsDayNight
@Composable
internal fun DpiSettingsViewPreview(@PreviewParameter(DpiSettingsStateProvider::class) state: DpiSettingsState) {
    ElementPreview {
        DpiSettingsView(
            state = state,
            onBack = {},
        )
    }
}
