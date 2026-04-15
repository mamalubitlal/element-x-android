# DPI Bypass Tests

Tests for the DPI (Deep Packet Inspection) bypass feature in Чатор.

## Test Files

| File | Description |
|------|-------------|
| `loginAndEnableDpiBypass.yaml` | Full end-to-end test: login with recovery key → enable DPI bypass |
| `loginWithRetry.yaml` | Login with retry wrapper for CI resilience |
| `enableDpiBypass.yaml` | Enable DPI bypass (assumes already logged in) |
| `disableDpiBypass.yaml` | Verify DPI proxy is running (assumes settings open) |
| `openSettings.yaml` | Open settings from home screen |

## Running Tests

### Local (requires emulator)

```bash
# Install Maestro
curl -fsSL "https://get.maestro.mobile.dev" | bash

# Run specific test
maestro test \
    -e MAESTRO_APP_ID=io.element.android.x.debug \
    -e MAESTRO_USERNAME=your_username \
    -e MAESTRO_PASSWORD=your_password \
    -e MAESTRO_RECOVERY_KEY=your_recovery_key \
    .maestro/tests/dpi/loginAndEnableDpiBypass.yaml

# Run all DPI tests
maestro test \
    -e MAESTRO_APP_ID=io.element.android.x.debug \
    -e MAESTRO_USERNAME=your_username \
    -e MAESTRO_PASSWORD=your_password \
    -e MAESTRO_RECOVERY_KEY=your_recovery_key \
    .maestro/tests/dpi/
```

### CI/CD

The tests run automatically via `.github/workflows/maestro-local.yml` using:
- `MATRIX_MAESTRO_ACCOUNT_PASSWORD` (GitHub Secret)
- `MATRIX_MAESTRO_ACCOUNT_RECOVERY_KEY` (GitHub Secret)

## Required Environment Variables

| Variable | Description |
|----------|-------------|
| `MAESTRO_APP_ID` | App package ID (default: `io.element.android.x.debug`) |
| `MAESTRO_USERNAME` | Matrix username (without @ prefix) |
| `MAESTRO_PASSWORD` | Matrix password |
| `MAESTRO_RECOVERY_KEY` | Recovery key for identity verification |

## Test Flow

1. **Login**: Open app → "Войти" → fill credentials
2. **Identity**: Use recovery key to verify device
3. **Onboarding**: Dismiss analytics ("Не сейчас") and notifications prompts
4. **Navigation**: Home → Profile → Settings → Обход DPI
5. **Test & Enable**: Test strategies → Enable bypass → Verify running

## UI Strings (Verified)

All Russian strings are verified from code in `features/*/values-ru/translations.xml`:

| English | Russian | Key |
|---------|---------|-----|
| Sign in | Войти | `screen_onboarding_sign_in_manually` |
| Use recovery key | Использовать ключ восстановления | `screen_identity_confirmation_use_recovery_key` |
| Not now | Не сейчас | `action_not_now` |
| DPI Bypass | Обход DPI | `screen_dpi_title` |
| Test strategies | Тестировать стратегии | `screen_dpi_test_strategies` |
| Enable DPI bypass | Включить обход DPI | `screen_dpi_enable_title` |

## Troubleshooting

### Emulator Issues

If the emulator goes offline in CI:
```bash
# Restart emulator
adb kill-server
adb start-server
adb devices
```

### Test Timing

The test uses `extendedWaitUntil` with generous timeouts (30-60s) for slow networks and emulator startup.

### Recovery Key Format

The recovery key should be in the standard Matrix format: `AAAA BBBB CCCC DDDD EEEE FFFF GGGG HHHH`
