# чатор Project Conventions ("Signs")

## Tech Stack

- **Language:** Kotlin 1.9+, Java 17
- **Android:** API 21+, Target SDK 34
- **Build:** Gradle 8.x, AGP 8.x
- **Testing:** JUnit, Espresso (UI tests)
- **Lint:** Android Lint, ktlint
- **Type checking:** Kotlin compiler

## Code Style

- Use type hints on all functions
- KDoc for public APIs
- Max line length: 120 characters
- Follow Element X existing patterns
- Russian strings in `values-ru/strings.xml`

## Architecture Patterns

- **MVVM** — ViewModels for UI state
- **Compose** — Modern UI (not XML layouts)
- **WorkManager** — Background tasks (DPI testing)
- **SharedPreferences** — Simple persistence (strategies, settings)
- **Flow/StateFlow** — Reactive streams

## чатор Principles

### Simplicity Over Modularity
- One app that does everything > multiple apps
- NO OIDC/SSO complexity — simple username/password login
- Built-in DPI bypass — no separate apps needed
- Users shouldn't install components separately

### Privacy Without Complexity
- No Google services (Fdroid variant)
- But NOT at expense of UX
- Find integrated solutions that are both private AND simple

### Russian-First
- Everything in Russian by default
- Locale forced to ru-RU
- This is a Russian messenger for Russian users

### Blue is чатор
- Primary color: #389CFF
- Use consistently across all UI elements

### Element X > Classic
- Modern, beautiful, actively developed
- UI matters — users notice good design

## Important Signs (Learned Lessons)

### Login/Register Flow (OIDC-LESS)
- ✅ **Element Web style** — simple username/password (reference: `Login.ts`, `Login.tsx`)
- ❌ **NO OIDC/SSO** — too complex, breaks simplicity principle
- ✅ **Direct Matrix auth** — `POST /_matrix/client/v3/login` with `m.login.password`
- ✅ **Registration with UIA** — expect 401, handle auth stages (reference: `Registration.tsx`)
- ✅ **Server pre-configured** — chator.k.vu (no manual server picker)
- ✅ **Check `/.well-known`** — if no `m.authentication` field → use password flow
- ✅ **Store tokens** — `access_token`, `user_id`, `device_id`, `home_server`

### DPI Bypass Integration
- ✅ **Automatic testing** — first boot + network change
- ✅ **Per-network storage** — best strategy per WiFi SSID/carrier
- ✅ **71 strategies** — from proxytest_strategies.list
- ✅ **8 Matrix domains** — test against real servers
- ✅ **Background worker** — no UI blocking
- ✅ **Russian UI** — all strings localized

### Bash Script Gotchas
- ✅ Use `VAR=$((VAR + 1))` NOT `((VAR++))`
- ✅ Always run tests before marking item complete
- ✅ Clean up abandoned approach files before finishing
- ✅ Use `set -e` in scripts for error handling

### Build System
- ✅ Debug APK: `./gradlew assembleDebug`
- ✅ Release APK: `./gradlew assembleRelease` (needs signing)
- ✅ Test: `./gradlew test`
- ✅ Lint: `./gradlew lint`
- ✅ Install: `adb install app/build/outputs/apk/debug/app-debug.apk`

### OkHttp + Cyrillic
- ❌ OkHttp crashes on Cyrillic in HTTP headers
- ✅ Fix: Convert "чатор" → "Chator" for User-Agent header only
- ✅ UI still shows "чатор" (Russian)

### Element X Theming
- **Files:** `libraries/compound/.../SemanticColorsLight.kt` + `Dark.kt`
- **Change:** Replace `colorGreen*` → `colorBlue*` throughout
- **Blue shades:** Primary #389CFF, Dark #1E6FD9, Light #6BB3FF

## File Organization

```
chator/
├── app/src/main/
│   ├── kotlin/io/element/android/
│   │   ├── features/
│   │   │   ├── dpi/bypass/      # DPI bypass logic
│   │   │   ├── login/           # Login screen
│   │   │   ├── register/        # Registration screen
│   │   │   └── settings/        # Settings UI
│   │   └── ElementXApplication.kt
│   ├── assets/
│   │   ├── proxytest_strategies.list
│   │   └── proxytest_matrix.sites
│   └── res/values-ru/strings.xml
├── specs/                        # Ralph Loop specs
├── fix_plan.md                   # Task tracker
└── ralph.sh                      # Loop script
```

## Testing Requirements

- Unit tests for logic (ViewModel, Manager classes)
- UI tests for new screens (Compose testing)
- Integration tests for DPI bypass (network tests)
- All tests must pass before marking complete

## Localization

- All new strings go to `values/strings.xml` (English)
- Russian translation in `values-ru/strings.xml`
- Use `stringResource(R.string.name)` in Compose
- Default locale forced to ru-RU in Application.kt

## Git Workflow

- Branch: `develop` (auto-builds on push)
- Commit format: `ralph: iteration N - description`
- Push after each successful iteration
- GitHub Actions builds debug APK automatically
