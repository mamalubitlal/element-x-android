# Spec: ByeDPI Integration (Automatic DPI Bypass)

## Objective

Integrate automatic DPI bypass strategy testing into чатор Android using ByeDPI engine.

**Reference:** `../chator-dpi-tester/` (existing working implementation)

---

## Requirements

### 1. Core Components (Copy from chator-dpi-tester)

**Kotlin Files:**
| Source File | Destination | Purpose |
|-------------|-------------|---------|
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/manager/DpiStrategyManager.kt` | `app/src/main/kotlin/io/element/android/features/dpi/bypass/DpiStrategyManager.kt` | Strategy storage & apply |
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/network/NetworkChangeObserver.kt` | `app/src/main/kotlin/io/element/android/features/network/NetworkChangeObserver.kt` | WiFi/Mobile detection |
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/work/DpiAutoTestWorker.kt` | `app/src/main/kotlin/io/element/android/features/dpi/bypass/DpiAutoTestWorker.kt` | Background test worker |
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/utility/MatrixStrategyTester.kt` | `app/src/main/kotlin/io/element/android/features/dpi/bypass/MatrixStrategyTester.kt` | Core test logic |
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/utility/SiteCheckUtils.kt` | `app/src/main/kotlin/io/element/android/features/dpi/bypass/SiteCheckUtils.kt` | HTTP connectivity test |
| `chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/utility/StrategyResult.kt` | `app/src/main/kotlin/io/element/android/features/dpi/bypass/StrategyResult.kt` | Data classes |

**Assets:**
| Source | Destination |
|--------|-------------|
| `chator-dpi-tester/app/src/main/assets/proxytest_strategies.list` | `app/src/main/assets/proxytest_strategies.list` |
| `chator-dpi-tester/app/src/main/assets/proxytest_matrix.sites` | `app/src/main/assets/proxytest_matrix.sites` |

---

### 2. Dependencies (app/build.gradle.kts)

```kotlin
dependencies {
    // WorkManager for background DPI testing
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Gson for JSON strategy storage
    implementation("com.google.code.gson:gson:2.10.1")
}
```

---

### 3. Application Initialization (ElementXApplication.kt)

**Add to `onCreate()`:**

```kotlin
override fun onCreate() {
    super.onCreate()
    
    val prefs = getSharedPreferences("chator_prefs", MODE_PRIVATE)
    val isFirstBoot = prefs.getBoolean("first_boot", true)
    
    // Initialize DPI bypass
    val strategyManager = DpiStrategyManager(this)
    val networkObserver = NetworkChangeObserver(this)
    networkObserver.startMonitoring()
    
    // First boot: run full DPI test
    if (isFirstBoot) {
        Timber.i("🥞 First boot - scheduling full DPI test")
        DpiAutoTestWorker.scheduleFirstBootTest(this)
        prefs.edit().putBoolean("first_boot", false).apply()
    }
    
    // Network change handling
    applicationScope.launch {
        networkObserver.networkState.collect { state ->
            if (state is NetworkState.Changed) {
                handleNetworkChange(state.type, strategyManager, networkObserver)
            }
        }
    }
}

private fun handleNetworkChange(
    networkType: NetworkType,
    strategyManager: DpiStrategyManager,
    networkObserver: NetworkChangeObserver
) {
    val networkId = when (networkType) {
        NetworkType.WiFi -> networkObserver.getCurrentWifiSsid()
        NetworkType.Mobile -> networkObserver.getCurrentCarrier()
        else -> "default"
    }
    
    val savedStrategy = strategyManager.getBestStrategyForNetwork(networkType, networkId)
    
    if (savedStrategy != null) {
        Timber.i("🥞 Applying saved strategy for $networkType/$networkId")
        strategyManager.applyStrategy(savedStrategy)
    } else if (strategyManager.isAutoTestEnabled()) {
        Timber.i("🥞 No strategy for $networkType/$networkId - scheduling test")
        DpiAutoTestWorker.scheduleNetworkChangeTest(this, networkType.toString())
    }
}
```

---

### 4. Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

### 5. Russian Strings (values-ru/strings.xml)

```xml
<!-- DPI Bypass Category -->
<string name="dpi_bypass_category">Обход DPI</string>

<!-- Settings -->
<string name="dpi_auto_test_enabled">Авто-тест при смене сети</string>
<string name="dpi_auto_test_enabled_summary">Автоматически тестировать стратегии при переключении WiFi/Мобильная</string>
<string name="dpi_test_now">Тестировать стратегии</string>
<string name="dpi_test_now_summary">Протестировать стратегии обхода DPI</string>
<string name="dpi_test_running">Тестирование стратегий…</string>
<string name="dpi_test_progress">Тест стратегии %1$d/%2$d против %3$d доменов…</string>
<string name="dpi_test_complete">Тест завершён! Лучшая: %1$s (%2$d%%)</string>
<string name="dpi_first_boot_test">Оптимизация соединения для вашей сети…</string>
<string name="dpi_retest_button">Тестировать заново</string>
<string name="dpi_no_strategy">Нет сохранённой стратегии</string>
<string name="dpi_strategy_saved">Стратегия сохранена</string>
<string name="dpi_current_strategy">Текущая стратегия</string>
<string name="dpi_test_duration">Время теста: ~%1$d мин</string>

<!-- Notifications -->
<string name="dpi_test_notification">Тест DPI обхода</string>
<string name="dpi_test_complete_title">Тест DPI завершён</string>
<string name="dpi_test_complete_text">Лучшая стратегия: %1$s</string>
```

---

### 6. Settings UI Integration

**File:** `app/src/main/kotlin/io/element/android/features/settings/advanced/AdvancedSettingsView.kt`

```kotlin
@Composable
internal fun AdvancedSettingsView(
    // ... existing params
    onDpiTestClick: () -> Unit = {},
    onDpiAutoTestToggle: (Boolean) -> Unit = {},
    isDpiAutoTestEnabled: Boolean = false,
    currentStrategy: String? = null,
) {
    // ... existing settings
    
    PreferenceCategory(title = stringResource(R.string.dpi_bypass_category)) {
        // Current strategy display
        if (currentStrategy != null) {
            ItemPreference(
                title = stringResource(R.string.dpi_current_strategy),
                subtitle = currentStrategy,
                icon = Icons.Outlined.Shield
            )
        } else {
            ItemPreference(
                title = stringResource(R.string.dpi_no_strategy),
                subtitle = null,
                icon = Icons.Outlined.Shield
            )
        }
        
        // Auto-test toggle
        SwitchPreference(
            title = stringResource(R.string.dpi_auto_test_enabled),
            subtitle = stringResource(R.string.dpi_auto_test_summary),
            isChecked = isDpiAutoTestEnabled,
            onToggle = onDpiAutoTestToggle
        )
        
        // Manual test button
        ClickablePreference(
            title = stringResource(R.string.dpi_test_now),
            subtitle = stringResource(R.string.dpi_test_now_summary),
            icon = Icons.Outlined.PlayArrow,
            onClick = onDpiTestClick
        )
    }
}
```

---

## Acceptance Criteria

- [ ] All 6 Kotlin files copied and adapted to Element X structure
- [ ] Assets copied (71 strategies, 8 Matrix domains)
- [ ] WorkManager + Gson dependencies added
- [ ] ElementXApplication.kt initializes DPI bypass on first boot
- [ ] NetworkChangeObserver detects WiFi ↔ Mobile switches
- [ ] DpiStrategyManager saves/loads strategies per network
- [ ] DpiAutoTestWorker runs in background (no UI blocking)
- [ ] Settings UI shows DPI bypass category
- [ ] Russian localization complete
- [ ] First-boot test runs automatically (~5 min)
- [ ] Network change auto-test works (~2 min)
- [ ] Manual test from Settings works
- [ ] Strategy auto-apply on network switch (instant)
- [ ] Strategy expiry after 24 hours
- [ ] Notifications on test completion

---

## Implementation Notes

### DO:
- ✅ Copy existing working code from `chator-dpi-tester/`
- ✅ Adapt package names to Element X structure
- ✅ Use WorkManager for background execution
- ✅ Store strategies in SharedPreferences (per-network)
- ✅ Russian localization for all UI strings
- ✅ Handle network change events efficiently

### DON'T:
- ❌ Re-implement from scratch (copy working code!)
- ❌ Block UI during testing (use WorkManager)
- ❌ Test on every network change (check if strategy exists first)
- ❌ Forget to handle permissions
- ❌ Skip Russian localization

---

## Testing

**First Boot:**
1. Fresh install → app opens
2. Notification: "Оптимизация соединения для вашей сети…"
3. Wait ~5 minutes (71 strategies × 8 domains)
4. Best strategy saved
5. App works optimally

**Network Switch:**
1. Switch WiFi → Mobile (or vice versa)
2. App detects change
3. If strategy saved → apply instantly
4. If no strategy → quick test (~2 min)

**Manual Test:**
1. Settings → Advanced → DPI Bypass → "Тестировать стратегии"
2. Progress dialog shows
3. Complete → show result toast

---

## Performance Targets

| Test Type | Duration | Strategies | Domains |
|-----------|----------|------------|---------|
| First Boot | ~5 min | All 71 | 8 Matrix |
| Network Change | ~2 min | Top 20 | 8 Matrix |
| Manual (full) | ~5 min | All 71 | 8 Matrix |
| Manual (quick) | ~1 min | Top 10 | 4 Matrix |

---

## Related Files

**Reference Implementation:**
- `../chator-dpi-tester/FEATURE_SUMMARY.md`
- `../chator-dpi-tester/INTEGRATION_GUIDE.md`
- `../chator-dpi-tester/app/src/main/java/io/github/romanvht/byedpi/`

**Element X Integration:**
- `app/src/main/kotlin/io/element/android/features/dpi/bypass/`
- `app/src/main/kotlin/io/element/android/features/network/`
- `app/src/main/assets/`
- `ElementXApplication.kt`

---

## Success Metrics

- First-boot test completes without crashes
- Network change detection works reliably
- Strategy save/load persists across app restarts
- Auto-apply is instant (< 100ms)
- No UI blocking during tests
- 100% Russian UI coverage
- All tests pass (unit + integration + manual)
