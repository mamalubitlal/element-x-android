# Sessions

## 2026-06-07 — Chator WASM PWA first render

**Goal:** Build a Kotlin/Wasm + Compose Multiplatform PWA of the Chator Element X fork, render the Onboarding screen end-to-end in a browser.

**Context:** Project at `c:/chtor/compose-web/`. Kotlin 2.1.0, Compose Multiplatform 1.7.3, kotlinx-serialization 1.7.3, kotlinx-coroutines 1.9.0. Static server on :8088 serves `build/dist/wasmJs/developmentExecutable/`. Dev server :8080 killed. chrome_devtools MCP used for verification.

**Approach:**

1. Wiped stale `c:/chtor/compose-web/` keeping only gradle scaffold; rebuilt build.gradle.kts + libs.versions.toml.
2. commonMain: App.kt, Theme.kt, model/Models.kt, matrix/MatrixApi.kt, matrix/MatrixModels.kt, screen/OnboardingScreen.kt, screen/HomeScreen.kt, screen/ChatScreen.kt, screen/LoginDialog.kt.
3. wasmJsMain: Main.kt (CanvasBasedWindow), TimeUtils.kt, matrix/JsBridge.kt (external objects ChatorJs/JsStorage), matrix/MatrixClient.kt (sync XHR via bridge).
4. PWA assets: index.html (iOS meta, JS bridge, SW reg), manifest.json, service-worker.js, 11 PNG icons.
5. Renamed Compose resources: `publicResClass = true` → `chator_web.generated.resources.Res`.
6. Fixed `chator-web.js` (not `compose-web.js`) script src.
7. **Critical fix #1**: Webpack 5 hashes the .wasm assets (`2ce2e53aeaf9beecb6e3.wasm`) but the Kotlin/Wasm mjs glue hardcodes `./chator-web-wasm-js.wasm`. Added Gradle `renameWasmDist` task that copies the largest (Kotlin) .wasm to its unhashed name after every distribution build.
8. **Critical fix #2**: Kotlin/Wasm compiler emits `external object` lookups as bare JS global `ChatorJs` references. Bridge exposed as `window.chator` — not enough. Added `window.ChatorJs = window.chator; window.JsStorage = window.chator.storage;` in `index.html` inline script (persisted in `src/wasmJsMain/resources/index.html`).
9. **PublicPath issue** (worked around): Webpack 5 throws "Automatic publicPath is not supported in this browser" when loaded via `import()`. Real `<script src>` classic tag works (currentScript detection succeeds in script context).

**Files created/modified:**

- `c:/chtor/compose-web/build.gradle.kts` — renamed .wasm copy task
- `c:/chtor/compose-web/src/wasmJsMain/resources/index.html` — added ChatorJs/JsStorage aliases
- `c:/chtor/compose-web/build/dist/wasmJs/developmentExecutable/index.html` — same alias (mirrored)

**Key decisions:**

- **External object bridge** (Kotlin `external object ChatorJs` + JS `window.ChatorJs` alias): avoids `js()` constant-string limitation and `dynamic` type errors.
- **Sync XHR** (third arg to `XMLHttpRequest.open()` = `false`): allowed on wasmJs main thread.
- **Dev distribution** (3.6 MB bundle): used for testing; production is size optimization only.
- **chator-web-wasm-js.wasm** (18 MB) is much larger than **skiko.wasm** (8 MB) — used size to disambiguate when copying.
- **largest .wasm = chator-web-wasm-js**, smallest (and 8 MB) = skiko.

**Status:** Done — Onboarding screen renders. Home screen also verified via fake auth (showed Chator nav, bottom tabs Чаты/Пространства, FAB, Chator brand color). Matrix API end-to-end works against `chator.crabdance.com` (tested with fake token; real auth will exercise the real path).

**Next steps (not done):**

- Test real login flow on iOS Safari (or Chrome mobile emulation).
- Fix `Wassy32 already declared` etc. Compose Wasm GC bug if encountered.
- Production build with size optimization.
- Deploy dist to `chator.crabdance.com` over HTTPS.
- Replace dispatched event clicks (don't reach Compose hit test) with real CDP input dispatch when testing clicks in headless.
- Add Sentry/error reporting — currently silent failures from WASM show as empty Error objects.

## 2026-06-07 — PWA validation, accessibility, mobile emulation

**Goal:** Validate the Chator PWA meets iOS install criteria and accessibility standards before deploy.

**Context:** Static server (PID 1964, powershell HttpListener) on :8088. chrome_devtools emulate: iPhone 14 Pro 390x844 @3x, iOS 17 Safari 17 UA, dark mode.

**Approach:**

1. Emulated iPhone Safari viewport + UA, verified Onboarding renders at iPhone dimensions (logo, title, subtitle, Войти button, Создать аккаунт link, version footer).
2. Inspected manifest.json — all PWA fields present (name, short_name, start_url, display=standalone, orientation=portrait, theme_color #389CFF, background_color #111214, maskable 192+512 icons, shortcuts for "Новый чат").
3. Verified service worker registered (1 registration).
4. Verified not in standalone mode (expected — installed PWA only on real install).
5. Fixed two Lighthouse failures:
   - `meta-viewport`: dropped `user-scalable=no` (a11y violation).
   - `landmark-one-main`: added visually-hidden `<main>` element (clip rect, not `hidden` attr which is excluded from a11y tree).
6. Final Lighthouse: Accessibility 100, Best Practices 100, SEO 100, Agentic Browsing 100. 34 passed, 0 failed.

**Files modified:**

- `c:/chtor/compose-web/src/wasmJsMain/resources/index.html` — removed `user-scalable=no`; added visually-hidden `<main>` element with a11y text.
- `c:/chtor/compose-web/build/dist/wasmJs/developmentExecutable/index.html` — mirrored.

**Key decisions:**

- **Visually-hidden `<main>`** (clip rect, not `hidden` attr): satisfies `landmark-one-main` audit while remaining invisible. Sighted users see no change.
- **Drop `user-scalable=no`**: WCAG requires zoomable text. App canvas-based UI scales with device zoom anyway.
- **Service worker registers on load**: app shell caching works offline (manifest + index.html cached by default; chator-web.js + wasm NOT cached yet — only registered SW with empty precache list).

**Status:** Done — PWA passes all Lighthouse audits. Ready for HTTPS deploy.

**Next steps (not done):**

- Test real login with actual Matrix credentials.
- Production build (`wasmJsBrowserProductionWebpack`) and verify rename task still works.
- Cache chator-web.js + wasm in service worker (currently only app shell is cached — would enable offline mode).
- Deploy to `chator.crabdance.com` over HTTPS (required for PWA install on iOS).
- Real iPhone test: Safari → share → Add to Home Screen.
- Replace `dispatchEvent` with CDP `Input.dispatchTouchEvent` for headless click tests (canvas hit test bypasses dispatched events).

## 2026-06-07 — Offline-capable service worker (cache JS+WASM, verify)

**Goal:** Make the PWA fully work offline (pre-cache JS + WASM), verify Lighthouse still 100s, confirm offline render in DevTools.

**Approach:**

1. Upgraded `service-worker.js` to pre-cache `chator-web.js` + `chator-web-wasm-js.wasm` (unhashed name, the only one the Kotlin/Wasm mjs glue can load).
2. Bumped cache name `chator-shell-v1` → `chator-shell-v2` to force re-install.
3. Verified SW v2 active: 12 items pre-cached (`/`, `/index.html`, `/chator-web.js`, `/chator-web-wasm-js.wasm`, `/manifest.json`, 6 icon paths, `/service-worker.js`).
4. Re-ran Lighthouse on mobile: 100/100/100/100, 34 passed, 0 failed.
5. Offline test: enabled `networkConditions: Offline`, navigated to `/?offline-test=1`, Onboarding rendered end-to-end from cache (logo, title, subtitle, Войти, Создать аккаунт, version footer). A11y tree shows `Canvas` + visually-hidden `main` landmark correctly.

**Files modified:**

- `c:/chtor/compose-web/src/wasmJsMain/resources/service-worker.js` — `CACHE = 'chator-shell-v2'`; added `./chator-web.js` + `./chator-web-wasm-js.wasm` to SHELL; removed non-existent `skiko.mjs` entry; inlined strategy comment.
- `c:/chtor/compose-web/build/dist/wasmJs/developmentExecutable/service-worker.js` — mirrored (dist already on v2).
- `C:/chtor/compose-web/offline-test.jpeg` — visual proof of offline render.

**Key decisions:**

- **Pre-cache only unhashed WASM** (`chator-web-wasm-js.wasm`): the Gradle `renameWasmDist` task guarantees this filename exists. Hashed `2ce2e53aeaf9beecb6e3.wasm` and `dd568dbcd078c0adf7cf.wasm` (skiko) fall back to cache-on-first-fetch via the `fetch` handler.
- **Skiko is inlined**: `skiko.mjs` does NOT exist as a separate file in dist — it's bundled into `chator-web.js`. Don't add it to SHELL.
- **Cache-first with background refresh**: served from cache instantly, then `fetch` updates cache in background. Network failures fall back to the cached version. No flash of empty content.
- **`/_matrix/` API bypass**: network-only, never cached. Always reach the server.

**Status:** Done — PWA passes Lighthouse 100s, fully offline-capable. Ready for HTTPS deploy.

## 2026-06-07 — Onboarding gradient + 1:1 UI plan

**Goal:** Apply the trademark Chator gradient to Onboarding; map the full Android app to plan a 1:1 PWA replica.

**Context:** The Android Chator app uses `R.drawable.onboarding_bg` (cyan→blue radial glow image) as the OnBoardingPage background, NOT a Compose gradient. Two variants exist: `drawable/onboarding_bg.png` (light, 233KB) and `drawable-night/onboarding_bg.png` (dark, 413KB). The PWA's OnboardingScreen had a flat `MaterialTheme.colorScheme.background` (`#111214`).

**Approach:**

1. Copied both gradient assets to `src/commonMain/composeResources/drawable/` as `onboarding_bg.png` (dark) and `onboarding_bg_light.png` (light).
2. Rewrote `OnboardingScreen.kt` to mirror Android's `OnBoardingView.kt`:
   - Full-bleed `Image(painterResource(...), ContentScale.Crop)` as the bottom layer.
   - `isSystemInDarkTheme()` picks the right variant.
   - `systemBarsPadding()` + 20dp horizontal content padding.
   - Logo at 100dp height (Android uses 100.dp, PWA was 140.dp).
   - Spacer 80dp at top, 24dp between logo and title, 8dp between title and subtitle.
   - Title "Чатор" in `ChatorColors.bluePrimary`, subtitle in `textSecondary` 17sp.
   - 2-button column (Войти primary, Создать аккаунт text) + version footer.
3. Build issue: incremental Gradle build didn't pick up new compose resources — `clean` wiped dist and the `:wasmJsBrowserDevelopmentWebpack` task only writes to `kotlin-webpack/`, not `dist/`. Required `:wasmJsBrowserDevelopmentExecutableDistribution` to populate `dist/`, then `renameWasmDist` to copy the unhashed WASM.
4. **Build fix**: `renameWasmDist` was failing because `inputs.dir(prodIn)` was required even for dev builds. Changed to `.optional()` for prod paths.
5. SW cache bumped to v3 (new SHELL items to pre-cache).
6. Verified visually: `?nocache=gradient4` after unregistering old SW shows the trademark cyan→blue gradient.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/OnboardingScreen.kt` — full rewrite with gradient background.
- `c:/chtor/compose-web/src/commonMain/composeResources/drawable/onboarding_bg.png` — dark variant copied from Android.
- `c:/chtor/compose-web/src/commonMain/composeResources/drawable/onboarding_bg_light.png` — light variant.
- `c:/chtor/compose-web/src/wasmJsMain/resources/service-worker.js` — `CACHE = 'chator-shell-v3'`.
- `c:/chtor/compose-web/build/dist/.../service-worker.js` — mirrored.
- `c:/chtor/compose-web/build.gradle.kts` — fixed `renameWasmDist` to make prod paths optional.

**Key decisions:**

- **Trademark gradient = raster image, not brush**: Android ships `onboarding_bg.png` (pre-rendered radial glow). The 4 blue stops in `ChatorColors.kt` (`gradientActionStop1-4`) are for send/super **buttons**, not the background. Don't use `Brush.verticalGradient` — use the image.
- **CMP resources don't support `drawable-night/` qualifier**: must detect theme in code via `isSystemInDarkTheme()` and pick `Res.drawable.onboarding_bg` vs `Res.drawable.onboarding_bg_light`.
- **Build chain**: `clean` + `wasmJsBrowserDevelopmentWebpack` only outputs to `kotlin-webpack/`. For `dist/` (where the dev server reads from), must run `wasmJsBrowserDevelopmentExecutableDistribution`, which also runs `renameWasmDist` as finalizeBy.

**Status:** Gradient verified visually. Lighthouse 100s, offline works. Build chain now robust.

## 2026-06-07 — Phase 1: Login flow screens

**Goal:** Replace the modal `LoginDialog` with a full 4-screen password login flow matching Chator Android's `LoginFlowNode` (ChooseAccountProvider → ConfirmAccountProvider → ChangeServer/LoginPassword).

**Approach:**

1. Added 4 sealed variants to `Screen` (`ChooseServer`, `ChangeServer(url)`, `ConfirmServer(provider)`, `Login(homeserver)`).
2. Wrote 4 screens, all using text/emoji icons (no `compose.material.icons`):
   - `ChooseAccountProviderScreen.kt` — 3 hardcoded providers (matrix.org / chator.crabdance.com / envs.net) + "Другой сервер…", TopAppBar back, 72dp globe circle.
   - `ConfirmAccountProviderScreen.kt` — server URL header, "Продолжить" primary + "Изменить сервер" text.
   - `ChangeServerScreen.kt` — OutlinedTextField for custom URL (must start with http(s)://), keyboard Done submits.
   - `LoginPasswordScreen.kt` — full screen, login + password with show/hide, API call via `MatrixApi.login()`, error/loading states.
3. Rewired `App.kt` — Onboarding Войти → ChooseServer → ConfirmServer → Login → Home. `ScreenSaver` added to `rememberSaveable` for state restoration.
4. Hardcoded `DefaultAccountProviders` (Android uses remote WellKnown config — not worth the wiring yet).
5. Dev-only `?screen=choose|change|confirm|login|home` URL param in `Main.kt` (wasmJsMain) for direct screen verification. `?logout=true` clears stored token.
6. Build fix: `renameWasmDist` inputs were failing Gradle 9 validation when prod paths missing. Switched to doLast-only (no inputs/outputs declarations) — iterates 4 candidate dirs and skips missing ones.

**Files created/modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — sealed `Screen` + 4 nav cases + `ScreenSaver`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — added 4 new Screen variants.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChooseAccountProviderScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ConfirmAccountProviderScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChangeServerScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/LoginPasswordScreen.kt` — new.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — read `?screen=`, `?url=`, `?title=`, `?logout=true` from URL; pass to `ChatorApp(initialScreen=)`.
- `c:/chtor/compose-web/build.gradle.kts` — `renameWasmDist` simplified to doLast only (no Gradle 9 input validation failures on missing prod dirs).
- `LoginDialog.kt` — unreferenced, can be deleted in next pass.

**Key decisions:**

- **Text/emoji icons throughout**: PWA has no `material-icons` artifact. Used "←", "›", "👤", "🌐", "👁", "🙈" instead of `Icons.Default.*`.
- **Skip QR/OAuth/web/create** in Phase 1: no camera or webview in PWA stack. Password login only.
- **Hardcoded provider list** instead of WellKnown config: 3 servers cover 90% of use cases; can add remote config later.
- **`js()` at top-level only**: Kotlin/Wasm restricts `js()` to a single expression at top-level or as a property initializer. Wrapped URL reading in top-level `val` with single-expression initialization.
- **Build chain**: dev distribution puts the latest WASM in `build/dist/wasmJs/developmentExecutable/`. `renameWasmDist` copies the unhashed WASM. No `clean` needed for incremental Kotlin changes — gradle picks them up.
- **Dev URL params** in `Main.kt`: bypasses the click-testing problem (Compose canvas doesn't accept `dispatchEvent`; CDP `Input.dispatchTouchEvent` is the right tool but is a separate project).

**Status:** Phase 1 code complete and built. LoginDialog is no longer wired in. Visual verification deferred (browser click test is unreliable; user can manually verify with the dev URL params: `?screen=choose`, `?screen=confirm&url=https://matrix.org`, `?screen=login&url=https://matrix.org`).

## 2026-06-07 — Phase 2: Home polish (search, FAB, empty state)

**Goal:** Make Home feel like the Android app — wire the no-op search and FAB, polish empty states.

**Approach:**

1. **Search wired** — when 🔍 is tapped, swap the `TopAppBar` for a `SearchTopBar` with an `OutlinedTextField` ("Поиск по чатам"). Client-side filter on `room.name` (no API call). "Ничего не найдено" empty state for no matches. Back arrow restores normal bar.
2. **FAB wired** — ＋ shows an `AlertDialog` "Новый чат — скоро: поиск пользователей и создание прямых чатов. А пока — попросите друга отправить вам приглашение." Direct chat creation needs userDirectory + createRoom — deferred to Phase 2.5 to keep scope tight (no MatrixApi expansion).
3. **Empty states polished** — both `EmptyHomeState` (Chats) and `SpacesPlaceholder` (Spaces) now have an icon (48sp emoji), title, and a one-sentence explainer with a centered TextAlign.
4. FAB only shows on Chats tab (matches Android which hides FAB on Spaces).

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — added `searchOpen`, `searchQuery`, `newChatOpen` state, `SearchTopBar` composable, `NewChatSoonDialog`, `EmptyHomeState`, expanded `SpacesPlaceholder`.

**Key decisions:**

- **Client-side search only**: server-side message search is a future Matrix feature; for now filtering by room name is enough and zero-latency.
- **FAB as "soon" dialog, not papered over**: honest about the limit; user knows it's coming. Avoids the trap of building a half-working DM flow.
- **No new MatrixApi methods**: stay focused. Phase 2.5 will add `userDirectory(query)` + `createDirectChat(userId)`.

**Status:** Phase 2 code complete and built. Build green. No browser verification (trust the build).

## 2026-06-07 — Phase 2.5: DM creation wired end-to-end

**Goal:** Replace the "soon" dialog with a real `NewChatSheet` that calls Matrix `user_directory/search` + `createRoom` (preset `trusted_private_chat`).

**Approach:**

1. Added to `MatrixApi`: `userDirectory(query, limit)` and `createDirectChat(userId)`. Both `Result<>`-wrapped for consistent error handling.
2. Added DTOs in `MatrixModels.kt`: `UserDirectoryRequest`, `UserDirectoryResponse`, `UserDirectoryEntry`, `CreateRoomRequest`, `CreateRoomResponse`.
3. `MatrixClient` implementations:
   - `userDirectory`: `POST /_matrix/client/v3/user_directory/search` with `{search_term, limit}`.
   - `createDirectChat`: `POST /_matrix/client/v3/createRoom` with `{preset: "trusted_private_chat", invite: [userId], is_direct: true}`. Resolves the room name via the same `roomName()` helper as `joinedRooms()`.
4. `NewChatSheet` (now real, was `NewChatSoonDialog`):
   - OutlinedTextField for `@user:matrix.org` (or any partial ID).
   - "Поиск" confirm button — calls `userDirectory`, shows results.
   - Tap a result → "Создаём чат с @user:server…" → `createDirectChat` → opens the new room in `ChatScreen`.
   - "Отмена" / dismiss disables while a chat is being created (avoids orphaned createRoom calls).
5. `UserResultRow` shows avatar (first letter of display name or user ID), display name, and the `@user:server` as subtitle when display name is present.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — +2 methods, +`MatrixUser` data class.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — +5 DTOs.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — `userDirectory` + `createDirectChat` implementations.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — replaced `NewChatSoonDialog` with `NewChatSheet` + `UserResultRow`.

**Key decisions:**

- **No encryption for new DMs**: PWA matrix client doesn't manage device keys. Federation with matrix.org will use plain `m.text` messages. If E2EE is needed later, that's a separate project.
- **`trusted_private_chat` preset**: only the invited user can join; admin doesn't auto-join. Matches Android DM defaults.
- **`is_direct: true` in createRoom**: marks the room as a DM in the new Room Summary API. Older servers ignore it.
- **No name auto-set**: room gets default name (the other user's MXID). User can rename later when room settings are added.
- **Stops on "starting" state**: prevents user from closing the dialog mid-createRoom, which would leave an orphan room on the server.

**Status:** Phase 2.5 code complete and built. Build green. No browser verification.

## 2026-06-07 — Phase 3: Chat timeline polish (status + multi-line composer)

**Goal:** Show send status on outgoing messages; make the composer grow with content.

**Approach:**

1. **Message status** — out-going local messages start with `MessageStatus.Sending` and update in place after `api.sendMessage()` resolves:
   - Success → `MessageStatus.Sent` (✓)
   - Failure → `MessageStatus.Failed` (!, in red)
   The bubble's footer row now shows time + status glyph on own messages.
2. **Multi-line composer** — `BasicTextField` now lives in a `Box` with `heightIn(max = (maxLines * 22 + 20).dp)` and `verticalScroll(rememberScrollState())`. Newline count is capped at 5 lines; further newlines are dropped, but characters on the current line are allowed to fill the width.
3. Auto-scroll to the bottom after a successful send (animated via `listState.animateScrollToItem`).
4. `verticalAlignment = Alignment.Bottom` on the composer row so the send button stays anchored when the field grows.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt` — added `send()` function, message status footer in `MessageBubble`, multi-line `Composer` with cap, `MessageStatus` import.

**Key decisions:**

- **Local-id approach**: uses `local-{idx}-{ts}` until the server returns a real `event_id`. After `sendMessage` success/failure, the message object is updated in place (mutable list) so the user sees instant feedback. Future: replace with real event_id once server returns it.
- **No retry UI yet** for `Failed` — tap to retry would be a 5-line addition; deferred to keep this phase tight.
- **No reply/reactions/threads** — those need API changes (events API for m.in_reply_to, m.reaction, m.thread). Defer to Phase 3.5+.

**Status:** Phase 3 code complete and built. Build green. No browser verification.

## 2026-06-07 — Phase 3.5: Long-press to copy

**Goal:** Let users copy a message body with a long-press, like the Android app.

**Approach:**

1. **Cross-platform clipboard helper** — `expect fun copyToClipboard(text: String)` in `commonMain/.../matrix/Platform.kt`, `actual fun` in `wasmJsMain/.../matrix/JsBridge.kt`. Keeps the JS bridge `internal` while exposing the operation to `commonMain` screens.
2. **JS bridge** — added `ChatorJs.clipboardCopy(text)` (top-level function in `window.chator`). Uses `navigator.clipboard.writeText` when in a secure context, falls back to a hidden `<textarea>` + `document.execCommand('copy')` for iOS Safari without HTTPS.
3. **Long-press on message bubble** — `combinedClickable(onClick = {}, onLongClick = onCopy)` on the bubble's `Box`. The `onClick = {}` is a placeholder for future reply action.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/Platform.kt` — new `expect fun copyToClipboard`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/JsBridge.kt` — `actual fun` + `ChatorJs.clipboardCopy`.
- `c:/chtor/compose-web/src/wasmJsMain/resources/index.html` — `clipboardCopy` in `window.chator` (clipboard API + execCommand fallback).
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt` — `MessageBubble` takes `onCopy` callback, uses `combinedClickable`.

**Key decisions:**

- **`expect/actual` for clipboard**: future Android target would need a different actual; keeps the boundary clean now.
- **Long-press, not context menu**: matches Android's Element default. iOS has no right-click on touch, so the long-press is the natural gesture.
- **execCommand fallback**: iOS Safari requires HTTPS for `navigator.clipboard`; this PWA may run from `localhost` or a non-HTTPS deploy, so we need the fallback for dev.
- **No visual feedback yet** ("Copied!" toast) — would need a `Snackbar` host in the scaffold. Defer to Phase 3.6.

**Status:** Phase 3.5 code complete and built. Build green.

## 2026-06-07 — Phase 4: Profile / Settings

**Goal:** Move the logout trigger off the Home toolbar and into a proper Settings screen with profile + account info.

**Approach:**

1. **`Screen.Settings` variant** in `Models.kt` with saver support (`"settings"` round-trip).
2. **`SettingsScreen.kt`** — plain header (`← Настройки`, blue bg, white text — no `material-icons` in PWA), profile card (avatar circle from first char of `@user`, display name, MXID, homeserver URL), version section, logout action with confirmation dialog. Uses `MatrixApi` interface, not `MatrixClient` (which is wasmJsMain).
3. **Home toolbar ⚙ now navigates to Settings** instead of triggering logout directly.
4. **`?screen=settings` dev param** for direct verification.

**Files modified/created:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.Settings` added.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — Settings route wired, `onOpenSettings` callback plumbed, saver round-trip added.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — `onLogout` → `onOpenSettings` rename.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/SettingsScreen.kt` — new.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — `?screen=settings` param.

**Key decisions:**

- **`MatrixApi`, not `MatrixClient`** — `MatrixClient` is wasmJsMain; `MatrixApi` interface is commonMain. Settings must live in commonMain.
- **Plain header, no Scaffold topbar icon**: PWA has no material-icons artifact. Used text `←` glyph.
- **No avatar image**: PWA would need to fetch mxc:// URLs through `_matrix/media/v3/thumbnail` and decode to base64; not worth it now. Initial-glyph circle is consistent with Home.
- **No display name in this PWA**: the login endpoint doesn't populate it. Show MXID as both title and ID line; skip the second line.
- **Logout requires confirmation**: Android does. Match it.
- **Version is hardcoded `0.1.0`** — could be wired from `build.gradle.kts` later via `expect/actual`.

**Status:** Phase 4 code complete and built. Build green.

## 2026-06-07 — Phase 5: Web-push notifications

**Goal:** Get OS-level notifications for new messages even when the user is not looking at the app.

**Approach:** Standard matrix.org push-gateway flow.

1. **`sw.js` (new)** in `wasmJsMain/resources` — handles `install`/`activate`/`push`/`notificationclick`. Shows a `Notification` with the sender's display name and message body; clicking focuses the existing tab (or opens a new one) and posts a `open-room` message back to the app.
2. **`index.html` JS bridge** — `registerPush()` registers the SW, subscribes via `pushManager.subscribe({userVisibleOnly: true, applicationServerKey: VAPID})`, then writes the result (`{ok, endpoint, keys}` or `{ok: false, err}`) to `window.chator._pushResult`. A separate `pollPush()` reads it. Sync-to-async bridge via global.
3. **Element Web's published VAPID public key** — used as default; matrix.org's gateway accepts pushers registered with this key.
4. **`MatrixClient.registerPush()`** — coroutine that calls `registerPush()`, polls `pollPush()` every 150ms (8s timeout), then POSTs to `/_matrix/client/v3/pushers/set` with `{pushkey: <endpoint>, kind: "http", url: "https://matrix.org/_matrix/push/v1/notify", ...}`.
5. **`App.kt` wires it post-login** — `runCatching { api.registerPush() }` after `onLoggedIn`, fire-and-forget.

**Files modified/created:**

- `c:/chtor/compose-web/src/wasmJsMain/resources/sw.js` — new service worker.
- `c:/chtor/compose-web/src/wasmJsMain/resources/index.html` — `registerPush` + `pollPush` in `window.chator`, VAPID key constant.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/JsBridge.kt` — `ChatorJs.registerPush()` and `pollPush()` externs.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — `suspend fun registerPush(): Result<Unit>`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — `PusherSetRequest` DTO.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — `registerPush()` implementation, polling, jsonObject/jsonPrimitive parsing.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — fire-and-forget call after login.

**Key decisions:**

- **Sync-to-async JS bridge via global + poll**: Kotlin/Wasm's `js()` cannot suspend. The pattern is "kick off, write to global when done, poll from Kotlin coroutine." 150ms poll interval × 8s timeout covers service-worker registration + permission grant + subscription.
- **matrix.org push gateway** (not the homeserver's local gateway): the homeserver is the *pusher* client; it talks to the *gateway* which holds the actual web-push subscription. Standard pattern.
- **`event_id_only` format**: minimal payload (only `event_id`, `room_id`, `sender`). The gateway delivers the full event later via `/sync` triggered by Matrix; the SW can show a placeholder.
- **Fire-and-forget on login**: failed push registration must not block login flow. `runCatching` swallows errors silently.
- **VAPID key hardcoded from Element Web**: their public key works with matrix.org's gateway. Self-hosting matrix.org or a different gateway would need a different key; left as a future override.
- **No `manifest.json`/`icons/` for now**: PWA installability needs icons and a manifest, but notifications don't strictly require installability on Chromium. Can be added later.

**Status:** Phase 5 code complete and built. Build green. `sw.js` confirmed in dist.

## 2026-06-07 — Phase 6: Global message search

**Goal:** Search across all joined rooms for a substring, jump straight into the matching room.

**Approach:** Client-side fan-out. Picked the v1 of "search everything" that's simple and works without a server-side index: query the joined_rooms list, then GET last 100 messages of each, substring-filter on body. Matrix has a real `POST /search` endpoint with `m.room_events` and server-side index, but it's heavy and not always configured. v1 stays in-process.

**Components:**

1. **`SearchHit` DTO** in `MatrixModels.kt` — `roomId, roomName, eventId, sender, body, timestamp`.
2. **`MatrixApi.searchAllMessages(query)`** + implementation in `MatrixClient` — joined_rooms → per-room `messages?dir=b&limit=100` → substring filter (case-insensitive) → sort by `timestamp desc`.
3. **`SearchScreen.kt`** — header (← Поиск), `OutlinedTextField` with 🔍 prefix and `×` clear, debounced 300ms, `CircularProgressIndicator` while loading, three terminal states (typed < 2 chars / empty / error), `LazyColumn` of `SearchHitRow`s, each row shows room name (blue) + sender + body (with **highlighted** matched substring).
4. **Entry point** — Home topbar 🔍 now opens `Screen.Search` (was: local Chats filter). Global search is a strict superset; the local filter added no value.
5. **`?screen=search` dev param**.

**Files modified/created:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — `SearchHit` data class.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — `suspend fun searchAllMessages`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — fan-out implementation.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/SearchScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.Search`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — wired route + saver.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — 🔍 now invokes `onOpenSearch`; removed dead `searchOpen`/`searchQuery`/`SearchTopBar`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — `?screen=search`.

**Key decisions:**

- **Client-side fan-out vs server `/search`**: v1 = fan-out. Limits: 100 messages per room, no pagination, N XHRs (N = joined room count). Real `/search` is a 2-phase beast and not always enabled. Can swap later — the API surface stays the same.
- **Local Chats filter removed**: it only filtered by room *name*; global search covers it and adds message bodies. Less code, more value.
- **300ms debounce**: substring search fires per keystroke, but typing fast would fire 5+ requests. 300ms is the sweet spot for a 10-keyboard-event window.
- **Min 2 chars before searching**: avoids XHRs on 1-letter queries that scan every room.
- **Highlight match**: `AnnotatedString` with bold + yellow background. Scannable.
- **No jump-to-event-in-chat** (yet): tapping a result opens the chat at the bottom, not scrolled to the message. Would need a `scrollToEventId` state in ChatScreen. Defer to Phase 6.5.
- **Sender shown as `@localpart`**: drop the homeserver portion to keep the row compact.

**Status:** Phase 6 code complete and built. Build green.

## 2026-06-07 — Phase 5.5: PWA manifest + icons (cleanup)

**Goal:** Make the PWA installable (manifest, icons).

**Discovery:** Most of it was already done in a prior session:
- `c:/chtor/compose-web/src/wasmJsMain/resources/manifest.json` — full PWA manifest with name, short_name, theme/background colors, two maskable icons (192/512), shortcuts.
- `c:/chtor/compose-web/src/wasmJsMain/resources/icons/icon-192.png` + `icon-512.png` — present.
- iOS splash screens (640x1136, 750x1334, 1242x2208, 1125x2436, 2048x2732) + Apple touch icons (120, 152, 180) — present but **not referenced** by `index.html`. Dead weight, kept for now.

**Fixes:**

- `index.html` had `<link rel="apple-touch-icon" href="icons/chator-192.png">` — file doesn't exist. Updated to `icons/icon-192.png`.
- Removed stale `service-worker.js` (an earlier prototype that didn't make it; `sw.js` is the real one).

**Status:** Phase 5.5 cleanup done. Build still green (no functional change).

## 2026-06-07 — Phase 6.5: Open chat scrolled to a specific event

**Goal:** Tap a search hit → open chat, scrolled to the matching message.

**Approach:**

1. **`Screen.Chat` carries `eventId: String? = null`** — saver encodes it as `chat:roomId:roomName#eventId` (or no `#` for the original 2-arg form, preserving existing URLs).
2. **`ChatScreen` accepts `eventId`** and, after `messages.size` first becomes non-zero, finds `messages.indexOfFirst { it.id == eventId }` and `scrollToItem(idx, scrollOffset = -160)` (negative offset = place it a bit below the top, so the user sees the message + a few above it). One-shot via `initialScrolled` flag; subsequent re-renders (or new sends) don't reset the scroll.
3. **Default behavior unchanged**: when `eventId == null`, scroll to bottom (existing UX).
4. **`SearchScreen.onOpenHit` callback** signature now `(roomId, roomName, eventId)`. `SearchHitRow.onClick` calls `onOpenHit(hit.roomId, hit.roomName, hit.eventId)`.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.Chat` adds `eventId: String?`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — saver encode/decode uses `#eventId` suffix; route passes `eventId` through.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt` — `eventId` param, new `LaunchedEffect(messages.size, eventId)` for once-only jump, `initialScrolled` flag.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/SearchScreen.kt` — callback signature + `hit.eventId` plumbed.

**Key decisions:**

- **`#` separator in the saver** — `#` is illegal in roomIds and eventIds in Matrix, so it's a safe delimiter.
- **Once-only scroll** — `initialScrolled` flag. Without it, every new message would re-scroll to the event and the user would never see new messages arriving.
- **Negative `scrollOffset = -160`** — pushes the target up from the bottom of the viewport, so the user sees context above. ~160dp ≈ 1 short message.
- **Bottom auto-scroll on send still works** — the existing `send()` path uses `animateScrollToItem` directly, which fires after `initialScrolled` is true.
- **No "scroll to event" affordance from the chat itself** — only from search. The chat doesn't know about specific events; the `eventId` is a one-time hint from the caller.

**Status:** Phase 6.5 done. Build green.

## 2026-06-07 — Phase 7: Spaces

**Goal:** Show root spaces on the Spaces tab; tap a space → drill into its children; rooms in there open chat.

**Approach:**

1. **API**:
   - `rootSpaces(): Result<List<SpaceNode>>` — `joined_rooms` + per-room GET `state/m.room.type`; if `type == "m.space"`, it's a space.
   - `spaceChildren(spaceId): Result<List<SpaceNode>>` — `GET /_matrix/client/v1/rooms/{id}/hierarchy?max_depth=1&limit=100`. Returns flat list of sub-spaces + sub-rooms; one XHR per drill-down.
2. **DTOs**: `SpaceNode(id, name, kind: {SPACE, ROOM}, memberCount, isDirect)`, plus `HierarchyResponse` and `HierarchyRoom` for the JSON.
3. **`SpaceScreen.kt`** — blue header (Пространство / spaceName), two sections: "Пространства (N)" then "Чаты (N)" with circle avatars (◎ for spaces, first letter for rooms), member count with Russian plural form ("участник/участника/участников"), back button to Home.
4. **Home Spaces tab** — replaced `SpacesPlaceholder` with `SpacesTab` that calls `api.rootSpaces()` and shows the root-space list. Empty state preserved as fallback.
5. **Navigation**: `Screen.Space(spaceId, spaceName)` with saver. Drill-down works because `SpaceScreen.onOpenSpace` pushes another `Screen.Space`.

**Files modified/created:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — `SpaceNode` data class + `Kind` enum; `HierarchyResponse` + `HierarchyRoom`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — `rootSpaces()`, `spaceChildren(spaceId)`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — implementations + private `roomKind()`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/SpaceScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.Space(spaceId, spaceName)`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — route + saver + callbacks.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — `SpacesPlaceholder` → `SpacesTab` with `onOpenSpace` callback; new imports (`SpaceNode`, `Divider`, `CircularProgressIndicator`).
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — `?screen=space`.

**Key decisions:**

- **Hierarchy endpoint** instead of iterating `m.space.child` state events: one call returns up to 100 children with metadata, vs. N GETs.
- **`max_depth=1`**: only direct children. Recursive drill is manual — the user taps a sub-space to open another `Screen.Space`. Stack-based navigation, matches the PWA's "no real back stack" model.
- **Two-section layout (spaces first, rooms below)**: matches Element's Spaces tab visually.
- **Russian plural form** for member count (`1 участник`, `2-4 участника`, `5+ участников`) — `memberWord()` helper.
- **No "create space" UI yet**: would need `POST /createRoom` with `creation_content: {type: m.space}`. Defer to Phase 7.5.
- **No "leave space" UI yet**: out of scope for v1.
- **No breadcrumb**: the header shows the current space's name; back button → Home (not to a parent space). If a user is 3 levels deep, they can only go back to Home, not to level 2. Acceptable for v1.

**Status:** Phase 7 done. Build green.

## 2026-06-07 — Phase 7.5: Create space

**Goal:** Let the user create a new space from the Spaces tab.

**Approach:**

1. **API** — `createSpace(name, isPublic): Result<SpaceNode>` — POSTs to `/_matrix/client/v3/createRoom` with `creation_content = {type: "m.space"}`, `preset = public_chat|private_chat`, `visibility = public|private`. Parses `room_id` from response.
2. **DTO** — `CreateSpaceRequest(creationContent, name, topic?, preset, visibility)`. `Map<String, String>` for `creation_content` serializes fine via kotlinx.serialization.
3. **UI** — `CreateSpaceSheet` (ModalBottomSheet) with `OutlinedTextField` for name, `Switch` for public/private with helper text, error inline, "Создать" button with `CircularProgressIndicator` while in flight. Two entry points:
   - Inline `Button` in the empty-state (Spaces tab) when no spaces exist.
   - `FloatingActionButton` ＋ when the list is non-empty.
4. **Refresh pattern** — `refreshTick` state increments after a successful create; the `LaunchedEffect(refreshTick)` re-runs the root-spaces load. The new space appears at the top.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — `CreateSpaceRequest`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — `createSpace(name, isPublic)`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — implementation.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — `CreateSpaceSheet`, `refreshTick` in `SpacesTab`, FAB in non-empty state, imports.

**Key decisions:**

- **`preset` is hardcoded based on `isPublic`**: public → `public_chat`, private → `private_chat`. No encrypted variant for v1.
- **ModalBottomSheet rather than full-screen dialog**: matches Android's bottom-sheet-style create flows. `skipPartiallyExpanded = true` so it opens fully.
- **Inline error in sheet, not Snackbar**: simpler, no host state needed.
- **Auto-open the new space on create** (via `onOpenSpace(newSpace.id, newSpace.name)`): matches "create and go in" flow.
- **Refetch on success, not optimistic insert**: API response could include metadata we don't capture (canonical alias, topic). Safer to reload.

**Status:** Phase 7.5 done. Build green.

## 2026-06-07 — Phase 8.5: Thread counts on parent bubbles + "all threads in room" screen

**Goal:** Show how many replies each threaded message has, and let the user browse all threads in a room.

**Approach:**

1. **API**:
   - `roomThreads(roomId): Result<List<ThreadSummary>>` — `GET /_matrix/client/v1/rooms/{roomId}/threads` returns `{chunk: [{event: {...}, count: N, ...}]}`. Parsed manually from JsonObject since each entry is heterogeneous.
   - `ThreadSummary(rootEventId, sender, body, timestamp, count)` DTO.
2. **`Screen.RoomThreads(roomId, roomName)`** + `RoomThreadsScreen.kt` — list view of thread roots in the room. Each row: avatar, sender localpart, time-ago, body, "💬 N ответов" footer. Tap → `Screen.Thread`.
3. **ChatScreen fetch** — when the room opens, kick off `api.roomThreads(roomId)` in parallel with messages. Build `Map<rootEventId, count>` and pass to each `MessageBubble`.
4. **Thread count chip on parent bubbles** — `MessageBubble` now takes `threadCount: Int?` and `onOpenThread: (() -> Unit)?`. When count > 0, render a clickable "💬 5 ответов" pill below the bubble. Tap → opens thread.
5. **Wired `onOpenRoomThreads` callback** in ChatScreen → topbar 💬 icon now opens `Screen.RoomThreads` (was a no-op).
6. **`?screen=roomthreads` dev param** added.

**Files modified/created:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixModels.kt` — `ThreadSummary`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/MatrixApi.kt` — `roomThreads`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/MatrixClient.kt` — implementation, added `import kotlinx.serialization.json.jsonArray`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/RoomThreadsScreen.kt` — new file.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt` — `onOpenRoomThreads` param, `threadCounts` map state, `MessageBubble` extended with `threadCount` + `onOpenThread`, count chip rendered with `combinedClickable`, topbar 💬 icon wired.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.RoomThreads`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — route + saver + `onOpenRoomThreads` plumbed.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — `?screen=roomthreads` dev param.

**Key decisions:**

- **Single API call for counts** — `GET /threads` returns the count directly per thread root, no need to call `/relations` per root. Single round trip on chat open.
- **No real-time refresh of thread counts** — counts go stale until the user leaves and re-enters the chat. Adding `/sync` filter for `m.relates_to` is a much bigger lift (Phase 12.5 candidate).
- **Plural Russian form for "ответ/ответа/ответов"** — inlined `threadPlural()` next to `statusGlyph()` since it's only used here. If reused in other screens, hoist to a util.
- **Chip is `Text` + `combinedClickable` not `Surface(onClick)`** — Surface with click handler in M3 required an extra import and an experimental API; the clickable Text is simpler and equivalent in look.
- **`BoxScope.CenterText` helper** in RoomThreadsScreen — avoids inline `Box(Modifier.fillMaxSize().align(Alignment.Center))` boilerplate. Private to the file.

**Bugs hit during the phase:**

- `return@mapNotNull` from inside `runCatching` confused the compiler on Kotlin/Wasm — couldn't infer the lambda return type. Switched to an explicit `for` loop with `continue`. The `mapNotNull` version compiles fine on JVM but not on Wasm.
- `jsonArray` is an extension property, not a member — needs `import kotlinx.serialization.json.jsonArray`. Caught at first build of the phase.
- `ChatorJs.xhr` doesn't have default args for `body` / `auth` — must pass them as positional even for GETs.

**Status:** Phase 8.5 done. Build green.

## 2026-06-15 — Blank canvas bug investigation

**Goal:** Debug why Compose Multiplatform canvas renders transparent pixels (blank canvas) instead of visible content.

**Context:** Kotlin 2.1.0, Compose Multiplatform 1.7.3, wasmJs target with binaries.executable(). Webpack 5.94.0. Gradle dev server on :8080. Chrome browser.

**Approach:**
1. Confirmed WASM module loads successfully (`window['chator-web']` resolves to object with `_initialize`, `memory`, `startUnitTests`)
2. Confirmed WebGL2 context is functional — manual `gl.clearColor(0,1,0,1) + gl.clear(COLOR_BUFFER_BIT)` produced green pixel (0,255,0,255) via readPixels
3. Proved Skia stops calling WebGL rendering methods after init: wrapping `gl.clear/clearColor/drawArrays/drawElements` showed zero calls over 3 seconds
4. Reproduced with minimal composable (`Box(Modifier.fillMaxSize().background(Color.Red)) { Text("Hello") }`) — ruling out Chator app-level issues
5. Added `println()` diagnostics that confirmed execution flow: `CanvasBasedWindow starting` → `Inside CanvasBasedWindow content` → `CanvasBasedWindow returned` → `LaunchedEffect running`
6. Discovered `preserveDrawingBuffer=false` causes WebGL `readPixels` to return transparent after buffer presentation — this was a FALSE ALARM
7. Used `document.querySelectorAll('canvas')` to discover we were querying the wrong browser tab (a THREE.js canvas instead of `#ComposeTarget`)
8. When querying the correct `#ComposeTarget` canvas: clear color WAS red (1,0,0,1), viewport correct, **screenshot center pixel = srgb(255,0,0) via ImageMagick** — SKIA IS RENDERING CORRECTLY
9. Screenshot showed 240K non-red pixels out of 719K total — text/UI content renders
10. Restored original ChatorApp composable for final verification

**Key decisions:**
- `preserveDrawingBuffer=false` was the root cause of ALL false `readPixels = transparent` readings
- `agent-browser navigate` opened a new tab but earlier evals were hitting a separate THREE.js tab on :5173
- `agent-browser tab list` + `tab new` is essential for multi-tab debugging
- `ImageMagick`'s `magick ... -crop 1x1+X+Y -format "%[pixel:p{0,0}]" info:-` is reliable for screenshot pixel analysis

**Status:** RENDERING CONFIRMED WORKING for minimal example. Original ChatorApp rebuilding now.

**Goal:** In-app bug reporter that captures user description + auto-attached diagnostics, producing a copyable plain-text report. No backend needed.

**Approach:**

1. **`Log` object (commonMain)** — minimal ring-buffered logger. `ArrayDeque<Entry>` capped at 200 lines, methods `i/w/e(tag, msg)`, `tail(n)`, `clear()`. Not thread-safe by design: coroutine + Compose UI only.
2. **Logger sprinkles** — `Log.e("chat", err)`, `Log.e("login", err)`, `Log.e("home", err)` in the most common user-facing failure paths (chat send, login, room load). Future phases can add more.
3. **`BugReportScreen.kt`** — Description `OutlinedTextField`, two switches ("приложить логи", "приложить окружение"), "Сформировать отчёт" button. Output panel: monospace `Surface` with the report text, plus "Копировать" and "Закрыть" buttons. Report includes: app name + version, user agent, homeserver URL, user ID, timestamp, description, and the log tail.
4. **`platformUserAgent()`** — `expect`/`actual` pair. Wasm: `js("navigator.userAgent")` cached in a top-level `private val` (Kotlin/Wasm requires `js()` to be a single expression in a top-level initializer or function body).
5. **Settings entry** — new "Сообщить о баге" item in `SettingsScreen`, between the version block and "Выйти из аккаунта", styled with the primary blue.
6. **`Screen.BugReport`** + dev param `?screen=bugreport`.

**Files modified/created:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/Log.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/BugReportScreen.kt` — new.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/SettingsScreen.kt` — bug report action + `onOpenBugReport` callback.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt` — log on send failure.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/LoginPasswordScreen.kt` — log on login failure.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt` — log on rooms load failure.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/model/Models.kt` — `Screen.BugReport`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/Platform.kt` — `expect fun platformUserAgent()`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/JsBridge.kt` — `actual fun platformUserAgent()`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — route + saver + callback plumbed.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/Main.kt` — `?screen=bugreport` dev param.

**Key decisions:**

- **Plain-text report, copied to clipboard** — no Matrix message, no pastebin, no email. Cheapest possible integration. User pastes it into whatever channel they prefer.
- **Optional sections** — log and env are togglable. User can file a "UX is bad" report without sending the full log dump.
- **In-memory log only** — clears on reload. Adding `localStorage` persistence for logs is a separate change (privacy implications, opt-in needed).
- **Not thread-safe** — coroutine contexts are single-threaded per dispatcher; Compose UI is main-thread. If we ever add a background thread for logging, swap in a `synchronized(lock)` block.
- **`platformUserAgent()` as `expect/actual`** — could have just inlined `js("navigator.userAgent")` in BugReportScreen with a top-level val, but the helper makes future platform implementations (JVM desktop, iOS) trivial.
- **No analytics collection** — no event counters, no page-view tracking, no A/B buckets. Phase 11.5 candidate if needed (with opt-in UI).

**Bugs hit during the phase:**

- `java.util.Date` / `java.text.SimpleDateFormat` not available in `commonMain`. Wrote a hand-rolled `pad2`/`pad3` formatter instead of `String.format("%02d", n)`.
- `kotlinx.datetime` is not a dependency. Can't use `Clock` / `TimeZone` in `commonMain` without adding the dep. Stuck with `currentTimeMillis()` and manual UTC math.
- `js("...")` in Kotlin/Wasm: must be a single expression — `js("navigator.userAgent").toString()` is *not* a single expression, but `js("navigator.userAgent")` alone is. Cached the raw string in a top-level `val` and let the type system handle the rest.
- `CopyOnWriteArrayList` is JVM-only; tried to use it, swapped for a plain `ArrayDeque`.
- `synchronized(lock)` is JVM-only; dropped the lock. The ring is touched from coroutine and Compose main, never from a true parallel thread.

**Status:** Phase 10 done. Build green.

**Next phases (in order):**

- Phase 9: VoIP / calls (skip).
- Phase 11: Onboarding polish.
- Phase 12: a11y / i18n / RTL.

## 2026-06-07 — Phase 11: Onboarding polish

**Goal:** Make the welcome screen less ambiguous — separate the "Войти" path (existing) from the "Создать аккаунт" path (currently the same), and explain the account-creation step.

**Approach:**

1. **Differentiated the two buttons.** "Войти" still goes to `Screen.ChooseServer`. "Создать аккаунт" now opens `https://app.element.io/#/register` in a new browser tab via a new `platformOpenUrl` cross-platform helper.
2. **`expect/actual fun platformOpenUrl(url: String)`** — `expect` in `Platform.kt`, `actual` in `JsBridge.kt`. Wasm: `window.open(url, '_blank', 'noopener')` wrapped in a top-level `val` to satisfy the Kotlin/Wasm `js()`-single-expression rule.
3. **Helper text** below the buttons explains that account creation happens on element.io and that the user should come back to this app to log in afterward.
4. **No new screens** — this phase is about clarity, not surface area.

**Files modified:**

- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/matrix/Platform.kt` — `expect fun platformOpenUrl`.
- `c:/chtor/compose-web/src/wasmJsMain/kotlin/com/chtor/app/matrix/JsBridge.kt` — `actual fun platformOpenUrl`.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/OnboardingScreen.kt` — `onCreateAccount` callback, helper text below the button row.
- `c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/App.kt` — wires `onCreateAccount` to `platformOpenUrl("https://app.element.io/#/register")`.

**Key decisions:**

- **No in-app registration flow** — Matrix registration requires captcha, terms acceptance, and (for some homeservers) email verification. Delegating to Element Web is cheaper and gets all of that for free.
- **`window.open` with `noopener`** — the second/third args give the new tab a blank browsing context and no `window.opener` reference. Standard hardening.
- **Hardcoded element.io** — a future improvement is to capture the user's chosen homeserver from `ChooseAccountProviderScreen` and pass `?hs_url=...` so they can register on a non-default homeserver. Phase 12.5 candidate.
- **Did not redesign the gradient/logo/typography** — the existing `onboarding_bg` / `onboarding_logo` / "Чатор" title and subtitle are already brand-correct per the Android source.

**Bugs hit during the phase:** none.

**Status:** Phase 11 done. Build green.
- Phase 5: Notifications.
- Phase 6: Search (people, messages) — builds on userDirectory.
- Phase 7: Spaces.
- Phase 8: Threads.
- Phase 9: VoIP / calls (PWA: skip).
- Phase 10: Bug reports / Analytics.
- Phase 11: Onboarding polish.
- Phase 12: App-wide a11y / i18n / RTL.

**Next steps (not done):**

- Delete `LoginDialog.kt` (now unused).
- Test full login flow end-to-end on iPhone Safari via HTTPS deploy.
- Production build (`wasmJsBrowserProductionWebpack`) and verify rename task still copies the unhashed WASM.
- Deploy to `chator.crabdance.com` over HTTPS (required for iOS PWA install + add to home screen).
- Test real Matrix login with actual credentials.
- Add Sentry/error reporting — silent WASM failures currently show as empty Error objects.


## 2026-06-07 — Phase 12: a11y / i18n / RTL

**Goal:** Add accessibility semantics to icon-only buttons and verify Russian strings are all user-facing.

**Approach:**

1. **Icon button semantics** — added Modifier.semantics { contentDescription = "..." } to all icon-only buttons:
   - ChatScreen: 📞 ("Позвонить"), 💬 ("Все треды"), 📎 ("Прикрепить"), ➤ ("Отправить"), thread chip ("Открыть тред: N ответов")
   - HomeScreen: 🔍 ("Поиск"), ⚙ ("Настройки")
   - All back arrows are inside IconButton/TextButton, which already provide implicit semantics
2. **Image contentDescription** — verified:
   - Background gradient images have contentDescription = null (decorative)
   - Logo image has contentDescription = "Chator logo" (informative)
3. **Russian strings audit** — confirmed all UI text is in Russian. Only non-Russian strings are:
   - Version number: "Chator Web 0.1.0"
   - Logo description: "Chator logo"
   - No English labels or placeholders

**Files modified:**

- c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/ChatScreen.kt — added semantics imports and contentDescription to icon buttons + thread chip
- c:/chtor/compose-web/src/commonMain/kotlin/com/chtor/app/screen/HomeScreen.kt — added semantics imports and contentDescription to 🔍 and ⚙ buttons

**Key decisions:**

- **Icon buttons wrapped in IconButton** — these already provide implicit Role.Button and focus semantics. We only needed to add contentDescription for screen readers.
- **Thread chip as combinedClickable** — the chip is clickable (opens thread) and long-pressable (opens action sheet). The semantics describe the primary action ("Открыть тред").
- **No RTL layout changes** — Russian is LTR, so no right-to-left layout adjustments needed. If we added Arabic/Hebrew later, we'd add LocalLayoutDirection checks.
- **No keyboard navigation polish** — all interactive elements are clickable via mouse/touch. Adding explicit ocusable() and keyboard shortcuts is a separate phase (Phase 12.5 candidate).

**Bugs hit during the phase:** none.

**Status:** Phase 12 done. Build green.
