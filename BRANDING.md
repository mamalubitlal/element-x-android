# Chator Branding — Status & Remaining Work

> **Base:** `ff237943fe` (Element X Android fork)
> **Logo asset:** `C:\Users\богдан рогоза станис\Pictures\chator-logo-newer.png`

---

## Legend

| Icon | Meaning |
|------|---------|
| ✅ Done | Already applied, compiles |
| ❌ Missing | Not yet done |
| ⚠️ Partial | Partially done / has issues |

---

## 1. App Launcher Icons ✅

Already implemented in `appicon/chator/`:

| Density | Files |
|---------|-------|
| `mipmap-mdpi` | `ic_launcher_foreground_chator.png`, `ic_launcher_round_chator.png`, `ic_launcher_monochrome_chator.png` |
| `mipmap-hdpi` | same 3 files |
| `mipmap-xhdpi` | same 3 files |
| `mipmap-xxhdpi` | same 3 files |
| `mipmap-xxxhdpi` | same 3 files |
| `mipmap-anydpi-v26` | `ic_launcher.xml` + `ic_launcher_round.xml` (adaptive icon wrappers) |
| `drawable` | `ic_launcher_background_chator.xml` (solid `#1A1A2E` background) |

Wired in `app/build.gradle.kts:263`:
```kotlin
implementation(projects.appicon.chator)
```
Used on non-enterprise builds (`else` branch at line 261).

---

## 2. App Icon (`ic_launcher` top-level) ✅

The app's `AndroidManifest.xml` references `@mipmap/ic_launcher` which resolves to the chator module's adaptive icon (or to element's on older build flavors). Since `appicon.chator` is a dependency, its `mipmap-anydpi-v26/ic_launcher.xml` takes priority for API 26+.

---

## 3. Notification Icon ✅

**File:** `libraries/designsystem/src/main/res/drawable/ic_notification.xml`

Chator chat bubble icon (white `#FFFFFF` for proper Android notification tinting) at 32dp.

Used throughout the app for:
- Push notifications
- In-app notification badges
- Status bar notifications

---

## 4. Onboarding Logo — Fallback (`element_logo`) ✅

**File:** `libraries/designsystem/src/main/res/drawable/element_logo.png`

Chator launcher foreground image copied to shadow Compound's internal "E" logo. Android resource shadowing means a resource with the same ID in the local module takes priority over library resources.

This is the logo shown on the onboarding page when no `onboarding_logo` drawable exists.

---

## 5. Onboarding Logo — Full-screen (`onboarding_logo`) ✅

**Code:** `features/login/impl/.../OnBoardingLogoResIdProvider.kt`
```kotlin
val resId = context.resources
    .getIdentifier("onboarding_logo", "drawable", context.packageName)
    .takeIf { it != 0 }
```

The `keep.xml` at `features/login/impl/src/main/res/raw/keep.xml` is already set up to retain `@drawable/onboarding_logo`.

**Behavior when present (`onBoardingLogoResId != null`):**
- `OnBoardingPage`'s `renderBackground = false` (no animated background)
- `OnBoardingLogo()` composable shows the drawable fullscreen

**Behavior when absent (`onBoardingLogoResId == null`):**
- `renderBackground = true` (snake/particle animation background)
- `OnBoardingContent()` composable shows `ElementLogoAtom` + text

**Done:** PNG at `features/login/impl/src/main/res/drawable/onboarding_logo.png` — copied from the Chator launcher foreground icon (192×192). The `Image` composable scales it to fill the screen with 16dp padding. Consider replacing with a vector drawable later for sharper scaling on high-density screens.

---

## 6. Onboarding Screen Colors ✅

**Files:**
- `libraries/designsystem/.../colors/ChatorColors.kt` — new helper object with Chator palette
- `features/login/impl/.../OnBoardingView.kt` — title uses `ChatorColors.bluePrimary` instead of generic `textPrimary`

**Approach:** Created a `ChatorColors` helper (option 3 from original audit) in `libraries/designsystem/colors/` with the canonical palette. Onboarding title now renders in Chator blue (`#389CFF`). Secondary message text stays on `ElementTheme.colors.textSecondary` (handles light/dark correctly).

Custom Chator colors in `app/src/main/res/values/colors.xml`:
```xml
<color name="chator_blue_primary">#FF389CFF</color>
<color name="chator_blue_dark">#FF1E6FD9</color>
<color name="chator_blue_light">#FF6BB3FF</color>
<color name="chator_accent">#FF389CFF</color>
```

---

## 7. Custom Colors — Usage in Code ✅

**Progress:**
- `ChatorColors.kt` created in `libraries/designsystem/colors/` — canonical Kotlin palette
- `SemanticColors.chatorColorOverride()` extension function in same file applies Chator accent palette via `copy()`
- Wired in `MainActivity.kt`: `colors.light.chatorColorOverride()` and `colors.dark.chatorColorOverride()` passed to `ElementThemeApp`
- Overrides: accent backgrounds, borders, focus, icons, text, links, badges, info colors, and send/super button gradients
- All non-accent fields (grays, backgrounds, decorative colors) remain as Compound defaults
- Used in `OnBoardingView.kt` for the welcome title (from previous session)
- The 4 XML colors in `colors.xml` still unused from XML resources (only used via `ChatorColors` Kotlin object — no XML consumers exist yet)

---

## 8. Documentation ✅

| File | Status |
|------|--------|
| `CLAUDE.md` | Updated — Чатор branding section, ChatorColors.kt reference |
| `README.md` | Replaced with Chator-fork version — Chator header, CI badge, cleaned sections |
| `BRANDING.md` | THIS FILE — active tracking |

---

## 9. Homeserver Config ✅

`DefaultEnterpriseService.kt` at `features/enterprise/impl-foss/`:
- `defaultHomeserverList()` → `emptyList()` (returns empty list, so no server pre-selected)
- `isAllowedToConnectToHomeserver()` → `true` (allows manual entry of any server)

This is okay for now — users can enter `chator.duckdns.org` manually. Server auto-pick logic exists in `HomeserverResolver.kt` (from earlier Chator work).

---

## 10. Splash Screen ✅

Android 12+ `Theme.SplashScreen` API is fully configured:

| Property | Light | Dark |
|----------|-------|------|
| `windowSplashScreenBackground` | `#FFFFFFFF` (white) | `#FF101317` (dark) |
| `windowSplashScreenAnimatedIcon` | `@mipmap/ic_launcher_foreground_chator` | same |
| `postSplashScreenTheme` | `Theme.ElementX` | same |
| Status bar / Nav bar | `splashscreen_bg_light` | `splashscreen_bg_dark` |

Shows the Chator launcher foreground icon centered on the correct light/dark background during app startup.

---

## Implementation Priority

| Priority | Task | Status | File(s) |
|----------|------|--------|---------|
| 🔴 P0 | Notification icon → Chator | ✅ | `libraries/designsystem/.../ic_notification.xml` |
| 🔴 P0 | Element logo → Chator (fallback) | ✅ | `libraries/designsystem/.../element_logo.png` |
| 🔴 P0 | Onboarding logo for full-screen variant | ✅ | `features/login/.../onboarding_logo.png` |
| 🟡 P1 | Onboarding text colors | ✅ | `ChatorColors.kt`, `OnBoardingView.kt` |
| 🟡 P1 | Update CLAUDE.md | ✅ | `CLAUDE.md` |
| 🟢 P2 | Update README.md | ✅ | `README.md` |
| 🟢 P2 | Wire chator colors into theme | ✅ | `ChatorColors.kt`, `MainActivity.kt` |
| 🟢 P2 | Splash screen | ✅ | `themes.xml`, `colors.xml` |

## Key Diagrams

### Onboarding Screen Decision Flow

```
OnBoardingLogoResIdProvider.get()
         │
         ▼
    resId != null? ──✅──► renderBackground=false
         │                    show OnBoardingLogo(drawable)
         │
         ▼ no
    renderBackground=true
    show OnBoardingContent()
         │
         ├── ElementLogoAtom (R.drawable.element_logo — Compound "E" logo)
         └── textPrimary / textSecondary text
```

### Resource Shadowing

```
┌─────────────────────────────────────────────┐
│  libraries/designsystem/src/main/res/drawable │
│  ├── element_logo.xml  ← NEW: shadows      │
│  │                       Compound's "E"     │
│  └── ic_notification.xml ← EDIT: Chator    │
│                                            │
│  features/login/impl/src/main/res/drawable/ │
│  └── onboarding_logo.xml ← NEW: fullscreen │
└─────────────────────────────────────────────┘
```
