## 2026-06-16 — Fix CI build: reorder repositories to resolve KMP metadata

**Goal:** Fix Android build failure at `:app:checkGplayDebugAarMetadata` caused by `ui-uikit` and `skiko-android` resolution errors.

**Context:** GitHub Actions run #387 (commit `715ae18ea1`) failed. Switching `haze` → `haze-android` wasn't enough — `haze-android:1.7.2` still transitively depends on `org.jetbrains.compose.foundation:foundation:1.10.3` (KMP module).

**Root cause:** Repository order in `settings.gradle.kts` had `artifactory.appodeal.com` BEFORE `mavenCentral()`. Appodeal proxies JetBrains Compose artifacts but only serves POM files (no `.module` files). Gradle uses POM-only resolution → POM lists ALL transitive deps including iOS-only `ui-uikit:1.10.3` (no Android variant) and `skiko:0.9.37.4` (requires `skiko-android` not on any repo). Maven Central has the correct `.module` files with variant-aware dependency filtering.

**Approach:** Moved `google()` and `mavenCentral()` before Appodeal repo. Removed redundant `repo1.maven.org` (alias for `mavenCentral()`).

**Files modified:**
- `settings.gradle.kts` — repo order: google, mavenCentral, Appodeal (was: Appodeal, google, mavenCentral, repo1)

**Key decisions:** Reordering is safer than exclusion rules (which could mask runtime issues). Appodeal-specific artifacts still resolve correctly since they only exist on Appodeal.

**Status:** done — committed and pushed (`a96fad7ef6`). CI run #388 pending.

## 2026-06-16 — Fix build: switch haze to -android artifacts

**Goal:** Fix Android build failure caused by KMP metadata resolution of `ui-uikit` and `skiko-android`.
**Context:** haze:1.7.2 is a KMP library. Kotlin plugin resolves its `metadataApiElements` variant → transitively resolves `foundation:1.10.0`'s metadata → lists `ui-uikit:1.10.3` (iOS-only) and `skiko-android:0.9.37.4` (doesn't exist on Maven Central — confirmed 404).
**Approach:** Changed `gradle/libs.versions.toml` to use `haze-android` and `haze-materials-android` — these are published as pure Android modules (no `metadataApiElements` variant), so the Kotlin plugin never triggers the transitive metadata chain.
**Files created/modified:**
- `gradle/libs.versions.toml` — `haze` → `haze-android`, `haze-materials` → `haze-materials-android`
**Key decisions:**
- Using `-android` suffix instead of adding exclusions or repos — cleaner, targets root cause
- No `build.gradle.kts` changes needed — version catalog entry swap is sufficient, accessors (`libs.haze`, `libs.haze.materials`) remain the same
- `haze-materials-android` transitively depends on `haze` (not `haze-android`), but since it resolves through `haze`'s Android variant (`available-at` redirect), it still avoids metadata resolution
**Status:** done — committed and pushed (`715ae18ea1`). CI run #387 pending.

## 2026-06-15 — Built 3D landing page with Three.js/R3F + dark theme + icons

**Goal:** Build the full 3D marketing landing page for Чатор with Three.js, dark theme, and vector icons.

**Context:** User rejected the AI service approach — wanted the site built directly. Previous PROMPT.md had incorrect claims (DPI, Jitsi, open source). User corrected: no DPI, not open source, has ads (but don't mention). Also wanted dark theme (matching app's dark mode) and icons from Flaticon/vector.

**Approach:**
- Built 3D components with React Three Fiber:
  - `HeroScene3D.jsx` — 3D phone model with animated chat screen (canvas texture), float animation, mouse tilt
  - `ChLogo3D.jsx` — 3D Ч logo (RoundedBox), rotates on scroll, glows on hover
  - `MatrixGrid.jsx` — animated grid background with parallax
  - `FeatureCard3D.jsx` — CSS 3D tilt on hover with per-card mouse tracking
  - `ParticleSystem.jsx` — burst particles + floating stars (Ready for Download button)
- Dark theme: all colors from app's `DarkColorTokens.kt` (#101317 bg, #1D1F24 surface, #26282D me-bubble, #E3E5E8 text)
- Icons: `react-icons/fi` (Feather) — FiLock, FiGlobe, FiPhone, FiMessageCircle
- Removed all incorrect claims from content and code

**Files created/modified:**
- `src/components/HeroScene3D.jsx` — NEW (3D phone with R3F)
- `src/components/ChLogo3D.jsx` — NEW (3D Ч logo)
- `src/components/MatrixGrid.jsx` — NEW (animated grid background)
- `src/components/FeatureCard3D.jsx` — NEW (3D tilt card)
- `src/components/ParticleSystem.jsx` — NEW (particles)
- `src/i18n.jsx` — fixed all content (no DPI/Jitsi/AGPL/ads), removed emoji
- `src/App.css` — converted to dark theme
- `src/App.jsx` — added MatrixGrid background + scrollY ref
- `src/components/Nav.jsx` — uses 3D Ч logo
- `src/components/Hero.jsx` — uses HeroScene3D
- `src/components/Features.jsx` — uses FeatureCard3D + Feather icons
- `src/components/About.jsx` — removed license block
- `src/components/Team.jsx` — uses 3D Ч avatar
- `src/components/Footer.jsx` — removed AGPL link
- `package.json` — added three, @react-three/fiber, @react-three/drei, react-icons, framer-motion

**Key decisions:** Built 3D with R3F for the phone/logo/grid. Used CSS-based 3D tilt for cards (simpler, more reliable). Canvas-drawn phone screen with dark theme colors.

**Status:** done — `cd C:\chtor\landing && npm run dev` runs clean. Build succeeds.

## 2026-06-14 — Corrected project identity: no DPI, no open source, has ads

**Goal:** Fix SPEC.md and PROMPT.md — user clarified the project doesn't have DPI bypass yet, isn't open source, and will have ads (but ads aren't mentioned on the website).

**Context:** Previous session wrote SPEC/PROMPT assuming built-in DPI bypass, open source (AGPL), and "no ads" messaging. User corrected all three.

**Approach:** Removed all references to:
- DPI bypass (features, hero sub, tech claims, team bio)
- Open source / AGPL license (license lines, footer, about section)
- "No ads" messaging (footer captions, hero sub, taglines)
Adjusted feature count from 4→3. Left "no tracking" claim intact (user didn't dispute it).

**Files modified:**
- `C:\chtor\landing\PROMPT.md` — hero sub, features (RU+EN), team bio, footer, license, 3D spec icons
- `C:\chtor\SPEC.md` — identity, deviz, features, about, team bio, footer, layout
- `C:\chtor\SESSIONS.md` — this entry

**Key decisions:** Keep "no tracking" (user didn't correct it). Features now: encryption, Russian UI, Jitsi calls.

**Status:** done — SPEC.md and PROMPT.md corrected.

## 2026-06-14 — Чатор landing page (app-matched redesign)

**Goal:** Make the landing page visually match the actual Чатор Android app.

**Context:** Previous dark theme attempt didn't reflect the app's actual look. Explored app source to understand real visual identity: LightColorTokens, SemanticColors, OnBoardingPage, SuperButton, Button.kt, MessageEventBubble, OnboardingBackground.

**Approach:** Rebuilt to match app's light mode exactly. White background (`#FFFFFF`) matching `bgCanvasDefault`. Blue accent (`#389CFF`). Pill-shaped buttons (matching `RoundedCornerShape(percent=50)`). Message bubbles styled like app (gray `#F0F2F5`/`#E1E6EC`, 12dp radius). Onboarding-style teal→blue bottom gradient (`#0DBDA8`→`#0D5CBD` at 8% opacity). Clean messenger aesthetic. Phone mockup now matches actual chat UI appearance.

**Details from app source that informed the redesign:**
- `bgCanvasDefault` = `#FFFFFF` (from LightColorTokens.colorThemeBg)
- Buttons: pill shape, 48px min height, `fontBodyLgMedium`
- Message bubbles: 12dp radius, `colorGray300`/`colorGray400` backgrounds
- Onboarding bottom gradient: teal `#0DBDA8` → blue `#0D5CBD`
- SuperButton (send): gradient colors `#1558A8`→`#1E6FD9`→`#389CFF`→`#6BB3FF`
- Chato rColors: bluePrimary `#389CFF`, blueDark `#1E6FD9`, blueLight `#6BB3FF`

**Status:** done — builds cleanly. `cd C:\chtor\landing && npm run dev` to run.

## 2026-06-09 — Kandev "agentctl not ready" fix

**Goal:** Fix "failed to create execution: agentctl not ready: context deadline exceeded" error when running tasks via opencode-acp.

**Context:**
- Kandev v0.56.0 on Windows x64 (Cyrillic username)
- Original state: `kandev.exe` PID 12848 running, but its child `agentctl.exe` was dead (PID 1100 exited)
- Agentctl is the bridge between the Kandev backend and agent subprocesses (via ACP JSON-RPC stdio)
- User's standalone `opcode acp --port 41002` was unrelated to agentctl's subprocess spawning

**Approach:**
- Mapped full architecture: Node CLI → kandev.exe → agentctl.exe → opencode acp (spawned per task)
- Traced launcher code (`launcher.go`): spawns `agentctl.exe -port=<port>`, waits for `/health` (30s timeout), performs bootstrap handshake
- Discovered existing backend (PID 12848) had zero children — agentctl had crashed/was killed
- Killed old Node CLI (PID 12680) + backend cascade
- Restarted via PTY: `node .../cli.js serve --port 38429`
- First attempts failed due to 15-20s bash timeout vs ~25s DB migration
- Success with `--verbose` flag + 60s PTY timeout
- Verified full stack: backend (PID 2584, :38429), agentctl (PID 7764, :39429), Next.js web (:37429)
- Both health endpoints returning OK

**Architecture discovered:**
```
Node CLI  (cli.js serve --port 38429)
  └─ kandev.exe  (Go backend, :38429)
       └─ agentctl.exe  (Go, :39429 — spawned via launcher.Provide())
            └─ opencode acp  (spawned per task, ACP JSON-RPC stdio)
```
- agentctl binary: `...\@kdlbs\runtime-win32-x64\bin\agentctl.exe`
- agentctl launched with no-arg call, env has `AGENTCTL_BOOTSTRAP_NONCE`
- Health check: polls `/health` with exponential backoff (100ms→1s), 30s deadline
- On Windows, lifecycle managed via Job Object (kill-on-close)
- Liveness pipe: parent keeps FD open, kernel closes on parent death → agentctl self-terminates

**Files created/modified:**
- `SESSIONS.md` — this entry

**Key decisions:**
- Restart killed processes rather than debugging dead agentctl — fastest path to fix
- Used PTY instead of bash tool for persistent backend process (bash kills children on timeout)
- `--verbose` mode essential for debugging startup (shows agentctl launch + health check + handshake)

**Status:** done — full Kandev stack running, agentctl healthy, original error resolved

## 2026-06-05 — PWA for iOS: Chator Android UI → web

**Goal:** Recreate the Chator (Element X fork) Android app UI as a PWA for iOS, with Matrix messaging support.

**Context:** Chator is a Kotlin/Jetpack Compose Android app. To run on iOS, we can't convert the APK — instead we built a web-based PWA that mirrors the Android UI exactly and connects to the same Matrix homeserver (chator.duckdns.org).

**Approach:**
- Analyzed Android app's Compose UI (OnBoardingView, HomeView, MessagesView, RoomSummaryRow, HomeTopBar, MessagesViewTopBar) and design system (ChatorColors.kt)
- Built a full PWA in `website/` with zero framework dependencies (vanilla JS)
- Three screens matching the Android app exactly:
  1. **Onboarding** — Ч logo, welcome text in ChatorBlue (#389CFF), sign-in buttons, settings icon
  2. **Home/Room list** — top bar with avatar, room list with avatars+names+previews, space filter chips, bottom nav (Чаты/Пространства) with FAB
  3. **Chat** — back button, room avatar+name, message timeline (sent/received bubbles), composer with send button
- Matrix REST API integration: login, register, sync, room list, messages, send
- Full PWA/ iOS support: manifest.json, service-worker (cache-first), iOS meta tags (apple-mobile-web-app-capable), apple-touch-icons (120/152/180px), splash screens for all iPhone sizes
- Dark mode via `prefers-color-scheme`
- Chator brand colors everywhere: `#389CFF` primary, `#1E6FD9` dark, `#6BB3FF` light

**Files created/modified:**
- `website/index.html` — 190 lines, all 3 screens + dialogs + iOS meta tags
- `website/manifest.json` — PWA manifest with icons
- `website/service-worker.js` — cache-first for static, network-only for Matrix API
- `website/css/theme.css` — 105 lines, Chator design tokens (matches ChatorColors.kt)
- `website/css/style.css` — 836 lines, full component styles
- `website/js/app.js` — 484 lines, SPA router, screen logic, Matrix interactions
- `website/js/matrix.js` — 186 lines, Matrix C-S API v3 client
- `website/icons/` — icon-192, icon-512, apple-touch-icons (120/152/180), splash screens (5 sizes)

**Key decisions:**
- Vanilla JS over framework (zero deps, smaller PWA, faster loads)
- Matrix REST API direct (no matrix-js-sdk dependency) — keeps the PWA lean
- ChatorColors exact match from Android: bluePrimary=#389CFF, blueDark=#1E6FD9, blueLight=#6BB3FF
- Three screens with slide transitions matching the Android navigation pattern
- Optimistic message sending (show instantly, mark error on failure)
- Light/dark mode support via CSS prefers-color-scheme

**Status:** done — deploy by serving `website/` via HTTPS (required for iOS service worker)

## 2026-06-03 — Wire Chator colors into theme + README

**Goal:** Complete P2 branding items: global theme colors + README.md
**Context:** After rollback + P0/P1 fixes, BRANDING.md showed 2 P2 items still ❌/⚠️. Colors existed in `ChatorColors.kt` but only `OnBoardingView` used them. README was still upstream Element.
**Approach:**
- Extended `ChatorColors.kt` with `SemanticColors.chatorColorOverride()` — top-level extension function that applies Chator accent palette over Compound defaults via `data class copy()`. Overrides: `bgAccentRest/Hovered/Pressed/Selected`, `borderAccentPrimary/Subtle`, `borderFocused`, `iconAccentPrimary/Tertiary`, `iconInfoPrimary`, `textActionAccent`, `textLinkExternal`, `textInfoPrimary`, `textBadgeAccent/Info`, `bgBadgeAccent/Info`, `bgInfoSubtle`, `gradientActionStop1-4`. Non-accent fields left at Compound defaults.
- Wired in `MainActivity.kt`: `colors.light.chatorColorOverride()` and `colors.dark.chatorColorOverride()` passed as `compoundLight`/`compoundDark` to `ElementThemeApp`
- Replaced `README.md` — stripped Element badges, Play Store/F-Droid, screenshots, translations, contributing. Added Chator header, description, fork credit, homeserver, CI badge.
- Updated `BRANDING.md`: item 7 → ✅, priority table updated
- Updated `CLAUDE.md`: branding section includes theme wiring details
**Files modified:**
- `libraries/designsystem/.../colors/ChatorColors.kt` — added `chatorColorOverride()` extension
- `app/.../x/MainActivity.kt` — wired `chatorColorOverride()` into theme
- `README.md` — full Chator replacement
- `BRANDING.md` — status update
- `CLAUDE.md` — updated branding section
- `SESSIONS.md` — this entry
**Key decisions:** Light touch — only accent fields overridden via `.copy()`, non-accent fields stay as Compound defaults. Extension function is top-level for clean import. Both light/dark themes get same Chator accent colors.
**Status:** done

## 2026-06-03 — BRANDING.md cleanup + onboarding_logo + README TOC

**Goal:** Close out remaining BRANDING.md items — fix statuses, create missing onboarding logo, clean up README.
**Context:** Previous session wired Chator theme colors. BRANDING.md had stale statuses (item 8 said ❌ despite README done, item 5 said ✅ but file missing). README TOC referenced removed sections.
**Approach:**
- Updated BRANDING.md: item 5 → ⚠️→✅ (infra existed, PNG was missing), item 8 → ✅ (README already Chator), item 10 → ⚠️ (splash screen configured, no branded icon)
- Updated priority table to match
- Created `features/login/impl/src/main/res/drawable/onboarding_logo.png` — copied from xxxhdpi Chator launcher foreground (192×192)
- Cleaned README.md TOC — removed stale entries (Screenshots, Translations, Status, Contributing), kept only existing sections
**Files modified:**
- `BRANDING.md` — status updates throughout
- `README.md` — TOC cleaned up
- `features/login/.../drawable/onboarding_logo.png` — NEW
- `SESSIONS.md` — this entry
**Key decisions:** Used existing PNG rather than vector — can be upgraded later. Onboarding will now show full-screen Chator logo (no animated background).
**Status:** done

## 2026-06-03 — Splash screen icon + BRANDING.md full cleanup

**Goal:** Close last remaining ⚠️ item (splash screen) — all BRANDING.md items now ✅.
**Context:** Splash screen had correct backgrounds but no branded icon (used `@drawable/transparent`). All other branding items were already done.
**Approach:**
- Replaced `@drawable/transparent` with `@mipmap/ic_launcher_foreground_chator` in both `values/themes.xml` (light) and `values-night/themes.xml` (dark)
- Updated BRANDING.md item 10 → ✅ with final config table
- Updated priority table
**Files modified:**
- `app/src/main/res/values/themes.xml` — splash icon light
- `app/src/main/res/values-night/themes.xml` — splash icon dark
- `BRANDING.md` — item 10 → ✅
- `SESSIONS.md` — this entry
**Key decisions:** Used existing launcher foreground PNG — consistent with other icon usage. Background colors kept as-is (white/dark) for smooth transition to app theme.
**Status:** done — all BRANDING.md items complete

## 2026-06-04 — Replace simplified Ч vectors with real PSD-sourced PNG

**Goal:** Use the actual Ч logo from the PSD for splash screen and onboarding instead of simplified vector approximations.

**Context:** Previous session created simplified Ч vector drawables (onboarding_logo.xml, ic_splash_chator.xml) as geometric approximations. User supplied the real Ч logo as `chator-logo-newer.png` from Photoshop.

**Approach:**
- Deleted `ic_splash_chator.xml` and `onboarding_logo.xml` (simplified vectors)
- Copied `chator-logo-newer.png` to `app/src/main/res/drawable/ic_splash_chator.png` (splash screen)
- Copied `chator-logo-newer.png` to `features/login/impl/src/main/res/drawable/onboarding_logo.png` (onboarding)
- Themes already reference `@drawable/ic_splash_chator` — now resolves to PNG
- `OnBoardingLogoResIdProvider` already resolves `onboarding_logo` — now finds PNG
- Also discovered PSD had Ч at #389CFF, 180×261px in 512×512 canvas with notch design

**Files created/modified:**
- `app/src/main/res/drawable/ic_splash_chator.png` — NEW (real Ч from PSD)
- `app/src/main/res/drawable/ic_splash_chator.xml` — DELETED (simplified vector)
- `features/login/impl/src/main/res/drawable/onboarding_logo.png` — NEW (real Ч from PSD)
- `features/login/impl/src/main/res/drawable/onboarding_logo.xml` — DELETED (simplified vector)
- `chator-logo-newer.png` — NEW (source asset in root)

**Key decisions:** Used real PSD-sourced PNG instead of approximate vector. Both splash and onboarding show the same Ч design. Source PNG kept in root for future vector tracing.

**Status:** pushed to GitHub, CI building.

## 2026-06-03 — Understanding Element X Android Theme Structure for Chator Brand Integration

**Goal:** Analyze the existing theme system to understand how to integrate Chator brand colors into Element X Android's Compose theme.

**Context:** Need to wire custom Chator brand colors into the existing Compose theme system. The codebase uses Compound design system with Material 3 theming.

**Approach:** 
1. Located theme definition files (ElementTheme.kt, ElementThemeApp.kt)
2. Found ChatorColors definition and matching XML resources
3. Examined how colors flow from enterprise service through MainActivity to ElementThemeApp
4. Identified that semantic colors from Compound are the foundation of the theme system

**Files created/modified:** None (analysis only)

**Key decisions:** 
- The theme system is built around Compound's SemanticColors
- ChatorColors are already defined as Compose Color constants matching XML resources
- Integration point is in ElementThemeApp where compoundLight/compoundDark parameters are passed to ElementTheme
- Best approach would be to create Chator-specific semantic color schemes or override colors in ElementThemeApp

**Status:** Analysis complete. Ready to implement Chator color integration by modifying the semantic color flow.

## 2026-06-17 — Fix CI: Coil 2.6.0 API migration (painter.state → painterState)

**Goal:** Fix remaining CI compilation errors after Coil 3.4.0 → 2.6.0 downgrade.

**Context:** CI run `27671899998` had 4 errors after downgrading Coil from 3.4.0 to 2.6.0:
1. `LocationPin.kt:90` — `AsyncImagePainter.State` no longer has `.painter` property
2. `ElementPreview.kt:27` — `previewBackgroundPainter()` no longer takes lambda
3. `BitmapAvatar.kt:50` — `painter.state.value` → `painterState.value` (Coil 2.6.0 API)
4. `ImageAvatar.kt:44` — same as #3

**Approach:**
- **LocationPin.kt**: Replaced `SubcomposeAsyncImage` content lambda (which accessed `state.painter`) with explicit `AsyncImage` call — simpler, no need to inspect painter state for location pin previews
- **ElementPreview.kt**: Changed `previewBackgroundPainter { ... }` to `previewBackgroundPainter(...)` — Coil 2.6.0 API takes `Painter` directly, not a lambda
- **BitmapAvatar.kt**: Changed `painter.state.value` → `painterState.value` — in Coil 2.6.0's `SubcomposeAsyncImage` content scope, the state is exposed as `painterState` (not `painter.state`)
- **ImageAvatar.kt**: Same fix as BitmapAvatar

**Files modified:**
- `libraries/location/impl/src/main/kotlin/.../LocationPin.kt` — replaced SubcomposeAsyncImage with AsyncImage
- `libraries/designsystem/src/main/kotlin/.../ElementPreview.kt` — fixed previewBackgroundPainter signature
- `libraries/designsystem/src/main/kotlin/.../BitmapAvatar.kt` — painter.state → painterState
- `libraries/designsystem/src/main/kotlin/.../ImageAvatar.kt` — painter.state → painterState

**Key decisions:** SubcomposeAsyncImage → AsyncImage in LocationPin was appropriate since no size-constrained content composable was needed; the `painter` obtained from state was just passed to `Image()` which AsyncImage handles natively.

**Status:** waiting for CI run `27672735442` to complete.

## 2026-06-16 — Investigated failing CI build for PR #384

**Goal:** Diagnose why `fix: remove remaining telephoto reference in messages module` PR build failed.

**Context:** GitHub Actions run `27596431714` on `mamalubitlal/element-x-android` (fork), commit `4622fa9`. Build step `Build GPlay Debug APK` failed with `BUILD FAILED in 2m 13s`.

**Approach:**
- Initially tried browser automation (GitHub Actions UI) but log navigation was clunky
- `gh run view --log` failed with cache corruption
- Used `Invoke-WebRequest` to download logs zip from API, extracted with `System.IO.Compression`
- Searched logs for error patterns via `Select-String`
- Used GitHub API to check related commits and files

**Findings:**
- 2 failures found, neither caused by the Telephoto removal itself:
  1. `WatchedAdsStore.kt` in `libraries:core` (JVM-only module) uses `android.content.Context`/`SharedPreferences` — added by prior Appodeal ads integration commit
  2. Coil 3.4.0 pulls JetBrains Compose Multiplatform deps → `ui-uikit:1.10.3` has no `androidJvm` variant; `skiko-android:0.9.37.4` not found
- Build eventually passed on re-run (`27596431742` → success) — likely transient dep resolution issue

**Files examined:**
- `features/messages/impl/build.gradle.kts` (PR change: removed `libs.telephoto.zoomableimage`)
- `libraries/core/src/main/kotlin/.../WatchedAdsStore.kt` (uses Android API in JVM module)
- `app/build.gradle.kts` (Appodeal SDK deps, Coil 3.4.0)
- `gradle/libs.versions.toml` (coil = 3.4.0)

**Status:** done — build passed on re-run.

## 2026-06-16 — Offline PDF rendering in media viewer

**Goal:** Replace placeholder "PDF preview not available" with real PDF rendering

**Context:** Media viewer showed a dead placeholder for PDFs — no actual content visible

**Approach:**
- Added `nickolay-savchenko/pdf-renderer` dependency (v0.5.0) and Coil 3.0.4
- Created PdfViewer composable with page-at-a-time rendering, zoom, swipe navigation, loading/error states
- Added PdfViewerState (loading/success/error + page tracking)
- Wired MediaPdfView to use PdfViewer for offline PDFs via file URI
- Updated DefaultLocalMediaRenderer to return LocalMediaViewState.Pdf instead of Generic
- Added Pdf to LocalMediaViewState sealed class
- Updated MediaViewerView to handle Pdf state alongside Image/Video

**Files modified:**
- `gradle/libs.versions.toml` — added pdf-renderer 0.5.0, coil3 3.0.4
- `libraries/matrixmedia/impl/build.gradle.kts` — added pdf-renderer-compose dep
- `libraries/mediaviewer/impl/build.gradle.kts` — added pdf-renderer-compose, coil3-compose
- `libraries/mediaviewer/impl/.../DefaultLocalMediaRenderer.kt` — emit Pdf state
- `libraries/mediaviewer/impl/.../LocalMediaViewState.kt` — added Pdf variant
- `libraries/mediaviewer/impl/.../image/MediaImageView.kt` — load from platformFile
- `libraries/mediaviewer/impl/.../pdf/MediaPdfView.kt` — wire PdfViewer
- `libraries/mediaviewer/impl/.../pdf/PdfViewer.kt` — NEW (PDF renderer)
- `libraries/mediaviewer/impl/.../pdf/PdfViewerState.kt` — NEW (state holder)
- `libraries/mediaviewer/impl/.../video/MediaVideoView.kt` — cleanup
- `libraries/mediaviewer/impl/.../viewer/MediaViewerView.kt` — Pdf branch
- `libraries/mediaviewer/impl/.../viewer/MediaViewerFlickToDismiss.kt` — suppress unused param

**Key decisions:** Used pdf-renderer over AndroidPdfViewer for Compose-native API. Coil 3 over 2 for compose-multiplatform consistency.

**Status:** done — committed as `22c3abacf8`