# Spec: Strategy Picker UI (Compose)

## Objective

Create a Compose UI for viewing, selecting, and testing DPI bypass strategies.

---

## Requirements

### 1. Strategy Picker Screen

**File:** `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyPickerView.kt`

**UI Components:**
- List of all 71 strategies (lazy column)
- Each item shows:
  - Strategy name/flags (e.g., `--disorder 1 --auto=torst`)
  - Success rate (if tested, e.g., "95%")
  - Last tested timestamp
  - Checkmark if currently active
- Tap to select & apply strategy
- Long-press to view details

**State:**
```kotlin
data class StrategyPickerState(
    val strategies: List<StrategyItem>,
    val selectedStrategy: String?,
    val isTesting: Boolean,
    val testProgress: Int, // 0-100
    val errorMessage: String? = null
)

data class StrategyItem(
    val flags: String,
    val successRate: Float?, // 0.0 - 1.0
    val lastTested: Long?, // timestamp
    val isActive: Boolean,
    val networkId: String // e.g., "wifi_HomeWiFi"
)
```

---

### 2. Strategy Test Progress Screen

**File:** `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyTestView.kt`

**UI Components:**
- Progress bar (overall: X of 71 strategies)
- Current strategy being tested
- Domain test results (8 domains, show pass/fail)
- Cancel button
- Estimated time remaining

**State:**
```kotlin
data class StrategyTestState(
    val currentStrategyIndex: Int,
    val totalStrategies: Int,
    val currentStrategy: String,
    val domainResults: List<DomainResult>,
    val isCancelled: Boolean,
    val bestStrategySoFar: String?,
    val bestSuccessRate: Float
)

data class DomainResult(
    val domain: String,
    val success: Boolean,
    val responseTimeMs: Long
)
```

---

### 3. ViewModel

**File:** `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyPickerViewModel.kt`

```kotlin
@HiltViewModel
class StrategyPickerViewModel @Inject constructor(
    private val dpiStrategyManager: DpiStrategyManager,
    private val networkObserver: NetworkChangeObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(StrategyPickerState(emptyList(), null, false, 0))
    val state: StateFlow<StrategyPickerState> = _state.asStateFlow()

    fun loadStrategies() {
        viewModelScope.launch {
            val currentNetwork = networkObserver.getCurrentNetworkId()
            val strategies = dpiStrategyManager.getAllStrategiesWithStats(currentNetwork)
            _state.value = _state.value.copy(
                strategies = strategies,
                selectedStrategy = dpiStrategyManager.getCurrentStrategy()
            )
        }
    }

    fun selectStrategy(strategy: String) {
        viewModelScope.launch {
            dpiStrategyManager.applyStrategy(strategy)
            _state.value = _state.value.copy(selectedStrategy = strategy)
        }
    }

    fun startFullTest() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTesting = true)
            DpiAutoTestWorker.startManualTest(application)
        }
    }
}
```

---

### 4. Navigation Integration

**File:** `app/src/main/kotlin/io/element/android/features/settings/SettingsNavigation.kt`

```kotlin
sealed class SettingsRoute {
    object Advanced : SettingsRoute()
    object DpiBypass : SettingsRoute()
    object StrategyPicker : SettingsRoute()
}

// Add to NavGraph
composable<SettingsRoute.DpiBypass> {
    StrategyPickerView(
        viewModel = hiltViewModel<StrategyPickerViewModel>(),
        onBackClick = { navController.popBackStack() },
        onTestClick = { navController.navigate<SettingsRoute.StrategyTest>() }
    )
}
```

---

### 5. Bug Report Integration

**File:** `app/src/main/kotlin/io/element/android/features/bugreport/BugReportView.kt`

Add button for quick DPI retest:

```kotlin
@Composable
fun BugReportView(...) {
    // ... existing bug report fields
    
    // DPI Bypass section
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onDpiRetestClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dpi_retest_button),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.dpi_test_now_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null
            )
        }
    }
}
```

---

## Acceptance Criteria

- [ ] StrategyPickerView shows all 71 strategies in lazy column
- [ ] Each strategy shows success rate (if tested)
- [ ] Active strategy has visual indicator (checkmark/blue highlight)
- [ ] Tap on strategy → apply immediately
- [ ] StrategyTestView shows real-time progress
- [ ] Domain test results shown (8 domains, pass/fail icons)
- [ ] Cancel button stops test gracefully
- [ ] ViewModel survives configuration changes
- [ ] Navigation works (Settings → DPI Bypass → Strategy Picker)
- [ ] Bug Report has "Re-test DPI" button
- [ ] Russian localization complete
- [ ] Dark theme support

---

## Russian Strings

```xml
<!-- Strategy Picker -->
<string name="dpi_strategy_picker">Выбор стратегии</string>
<string name="dpi_strategy_list">Список стратегий</string>
<string name="dpi_strategy_active">Активная</string>
<string name="dpi_strategy_success">Успех: %1$d%%</string>
<string name="dpi_strategy_never_tested">Не тестировалась</string>
<string name="dpi_strategy_last_tested">Тест: %1$s</string>
<string name="dpi_strategy_apply">Применить</string>

<!-- Test Progress -->
<string name="dpi_testing_title">Тестирование стратегий</string>
<string name="dpi_testing_strategy">Стратегия %1$d из %2$d</string>
<string name="dpi_testing_domain">Домен: %1$s</string>
<string name="dpi_testing_success">✓</string>
<string name="dpi_testing_fail">✗</string>
<string name="dpi_testing_cancel">Отмена</string>
<string name="dpi_testing_eta">Осталось: ~%1$d мин</string>

<!-- Bug Report -->
<string name="dpi_retest_title">Перетестировать DPI</string>
<string name="dpi_retest_summary">Если соединение нестабильно</string>
```

---

## UI Mockup (Text)

```
┌─────────────────────────────────────┐
│ ← Обход DPI                         │
├─────────────────────────────────────┤
│                                     │
│  🛡️ Текущая стратегия              │
│     --disorder 1 --auto=torst      │
│     Успех: 95%                     │
│                                     │
│  ⚙️ Авто-тест при смене сети  [✓]  │
│                                     │
│  ────────────────────────────────   │
│                                     │
│  📋 Все стратегии (71)              │
│                                     │
│  ✓ --disorder 1          95%  [Active]
│    --split 1+s           87%        │
│    --fake -1 -ttl 8      78%        │
│    --disorder 3+s        72%        │
│    ...                              │
│                                     │
│  ────────────────────────────────   │
│                                     │
│  ▶ Тестировать стратегии            │
│     ~5 мин, 71 стратегий            │
│                                     │
└─────────────────────────────────────┘
```

---

## Related Files

**Create:**
- `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyPickerView.kt`
- `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyTestView.kt`
- `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyPickerViewModel.kt`

**Update:**
- `app/src/main/kotlin/io/element/android/features/settings/AdvancedSettingsView.kt`
- `app/src/main/kotlin/io/element/android/features/bugreport/BugReportView.kt`
- `app/src/main/kotlin/io/element/android/features/settings/SettingsNavigation.kt`

---

## Success Metrics

- Strategy list scrolls smoothly (60 FPS)
- Strategy apply is instant (< 100ms)
- Test progress updates in real-time
- No memory leaks (ViewModel cleared properly)
- 100% Russian UI coverage
- Dark/light theme both work
- All Compose previews render correctly
