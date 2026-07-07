# Chator PWA for iOS — Kotlin/Wasm Compose Multiplatform Build Prompt

## Goal
Build a **Progressive Web App (PWA) for iOS** from the **Chator Android app** (Element X fork, Matrix messenger) using **Kotlin/Wasm with Compose Multiplatform** — sharing actual Compose UI code between Android and web.

## Target
- **Homeserver**: `https://chator.space`
- **Pixel-perfect** match to Android Chator UI (colors, layouts, animations from `ChatorColors.kt`)
- **Full Matrix functionality**: login, register, sync, room list, messaging, send
- **iOS PWA compliance**: manifest, service worker, apple-mobile-web-app-capable, splash screens, touch icons

## Architecture
```
compose-web/                          # Standalone Gradle project (Kotlin/Wasm)
├── build.gradle.kts                  # Kotlin 2.1.20, Compose 1.7.3, wasmJs target
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── commonMain/kotlin/com/chtor/app/
│   │   ├── App.kt                    # Main app composable, navigation, sync loop
│   │   ├── Main.kt                   # (empty - entry is in wasmJsMain)
│   │   ├── Theme.kt                  # ChatorColors ported exactly from Android
│   │   ├── TimeUtils.kt              # expect/actual time formatting (no kotlinx.datetime)
│   │   ├── model/
│   │   │   ├── Screen.kt             # sealed class: Onboarding | Home | Chat
│   │   │   ├── ChatRoom.kt
│   │   │   └── Message.kt
│   │   ├── matrix/
│   │   │   ├── MatrixApi.kt          # Interface (commonMain)
│   │   │   ├── MatrixModels.kt       # Data classes for Matrix API
│   │   │   └── MatrixClient.kt       # (empty - impl in wasmJsMain)
│   │   └── screen/
│   │       ├── OnboardingScreen.kt   # Logo, title, 3 buttons, dev cog
│   │       ├── HomeScreen.kt         # Top bar, space filters, room list, bottom nav + FAB
│   │       ├── ChatScreen.kt         # Top bar, timeline (bubbles), composer
│   │       └── LoginDialog.kt        # Reusable dialog for login/register/server
│   └── wasmJsMain/
│       ├── kotlin/com/chtor/app/
│       │   ├── Main.kt               # CanvasBasedWindow entry point
│       │   ├── TimeUtils.kt          # actual impl using js("Date.now()")
│       │   └── matrix/
│       │       └── MatrixClient.kt   # XMLHttpRequest + suspendCoroutine impl
│       └── resources/
│           ├── index.html            # PWA shell with iOS meta tags
│           ├── manifest.json         # PWA manifest
│           ├── service-worker.js     # Cache-first static, network-only Matrix API
│           └── icons/                # Generated from Android playstore icon
│               ├── icon-192.png
│               ├── icon-512.png
│               ├── apple-touch-icon.png
│               ├── apple-touch-icon-120.png
│               └── apple-touch-icon-152.png
```

## Key Implementation Details

### Build System (build.gradle.kts)
```kotlin
plugins {
    kotlin("multiplatform") version "2.1.20"
    id("org.jetbrains.compose") version "1.7.3"
    kotlin("plugin.compose") version "2.1.20"
}
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}
```

### Matrix Client (wasmJsMain)
- Uses `XMLHttpRequest` + `suspendCoroutine` (no Promise/await complexity)
- JSON via `js("JSON.parse/stringify")` + dynamic access
- Endpoints: login, register, sync, messages, send
- Base URL: `https://chator.space`

### UI Screens (matching Android exactly)
| Screen | Android Source | Key Elements |
|--------|----------------|--------------|
| Onboarding | `OnBoardingView.kt` | Gradient bg, Ч logo, "Чатор" in #389CFF, 3 buttons, version |
| Home | `HomeView.kt` | Top bar (avatar, title, search, menu), space filter chips, room list (avatar, name, preview, time, unread dot), bottom nav (Чаты/Пространства + FAB) |
| Chat | `MessagesView.kt` | Back btn, room avatar+name, call/thread btns, message bubbles (sent right/blue, received left/white), day dividers, composer (attach, input, send) |

### Colors (from `ChatorColors.kt`)
```kotlin
bluePrimary = #389CFF
blueDark = #1E6FD9
blueLight = #6BB3FF
bluePressed = #1558A8
bgLight = #F5F5F0, bgDark = #111214
surfaceLight = #FFFFFF, surfaceDark = #1C1E21
textPrimaryLight = #1A1A1A, textPrimaryDark = #E4E4E7
```

### PWA/iOS Resources
- `index.html`: viewport-fit=cover, apple-mobile-web-app-capable, 5 splash screen sizes, 4 touch icon sizes
- `manifest.json`: standalone, portrait, theme_color #389CFF
- `service-worker.js`: cache-first for static, network-only for `chator.space/_matrix/*`

## Current Build Status
**FAILING** — Internal compiler error in `MatrixClient.kt:235` (type intersection issue with `when` expression on dynamic) + unresolved references in `App.kt` (`MatrixClient` not in commonMain) and `LoginDialog.kt` (`KeyboardOptions` not available in commonMain).

## Fixes Needed
1. **Move `MatrixClient` to commonMain** as `expect class` with `actual` in wasmJsMain, OR make `MatrixApi` the only common interface and instantiate `MatrixClient` only in wasmJsMain (pass to `ChatorApp` from `Main.kt`)
2. **Fix `MatrixClient.kt` type error** — simplify the `when` expression on dynamic, avoid smart-cast issues
3. **Remove `KeyboardOptions`/`KeyboardType`/`ImeAction` from commonMain** — not available in Compose Multiplatform common; use platform-specific or conditional imports
4. **Fix `App.kt`** — don't reference `MatrixClient` directly in commonMain; use `MatrixApi` interface only
5. **Ensure `CanvasBasedWindow` import works** — may need `androidx.compose.ui.window.CanvasBasedWindow`

## To Resume
```bash
cd c:/chtor/compose-web
./gradlew build
```

## Disk Space Note
C: drive was low (~1.2GB). Cleaned temp/Gradle caches → ~4GB free. Binaryen download for WASM needs space.

---

**Generated from session:** Built entire project structure from scratch, wiped old `website/` and `compose-web/src/`, wrote all Kotlin source, PWA resources, generated icons from Android playstore icon.