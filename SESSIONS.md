# Sessions

## 2026-05-30 — Strip DPI bypass

**Goal:** Remove all DPI bypass code (ByeDpiLibrary, native libs, DPI settings UI)
**Context:** Чатор fork of Element X Android. DPI bypass was a custom addition using `io.github.romanvht.byedpi:library:1.0.411`. No tokens to spare — caveman + cove active.
**Approach:**
- Removed `libraries/dpi/` (api + impl modules, native .so, AAR dep)
- Removed `features/preferences/impl/.../dpi/` UI package (Node, Presenter, View, State, Events)
- Removed `app/src/main/.../x/dpi/` app-level code
- Strip DPI from `DeveloperSettingsPresenter/State/Events/View` (injection, state, event handlers, private helpers)
- Strip DPI nav from `PreferencesFlowNode` (import, NavTarget, callback, resolver) + `PreferencesRootNode` (Callback, View)
- Strip `onOpenDpiSettings` from `PreferencesRootView`
- Remove DPI deps from `app/build.gradle.kts`, `features/preferences/impl/build.gradle.kts`
- Remove `flatDir` for `libraries/dpi/libs` from `settings.gradle.kts`
- Remove `pickFirsts += "**/libbyedpi.so"` from `app/build.gradle.kts`
- Remove `screen_dpi_*` strings from `localazy.xml` + `translations.xml`
- Remove DPI test files + Maestro DPI tests
- Fix `DeveloperSettingsPresenterTest.kt` references
**Files created/modified:** ~20 edited, ~40 deleted
**Key decisions:** Kept `connection_helper_strings.xml` (references ByeByeDPI as 3rd party app recommendation — zero code refs, dead UI copy only)
**Status:** done

## 2026-05-30 — Auto-pick fastest homeserver

**Goal:** App checks `85.209.2.14:8008` vs `chator.duckdns.org`, picks fastest
**Context:** Чатор fork — Russian users need fallback homeserver. Two server candidates: direct IP:port + DNS name.
**Approach:**
- Added `CANDIDATE_HOMESERVERS` list to `AuthenticationConfig.kt` (`http://85.209.2.14:8008`, `https://chator.duckdns.org`)
- Created `HomeserverResolver.kt` — pings both via `/_matrix/client/versions` with `HttpURLConnection`, sorts by latency, stores fastest URL in SharedPreferences via existing `AuthenticationConfig.setCustomMatrixUrl()`
- Wired into `ElementXApplication.onCreate()` — calls `AuthenticationConfig.init(this)` then launches resolver in `applicationScope` coroutine
- Resolver is idempotent: skips if custom URL already stored (from previous run or manual override)
- First launch uses default `DEFAULT_MATRIX_URL` from `chator-config.properties` until resolver completes; subsequent launches load cached URL immediately
**Files created/modified:**
- `appconfig/.../AuthenticationConfig.kt` — added `CANDIDATE_HOMESERVERS`
- `app/.../x/HomeserverResolver.kt` — new resolver
- `app/.../x/ElementXApplication.kt` — wired init + resolver launch
**Key decisions:** Used `java.net.HttpURLConnection` to avoid extra deps. Resolver in app module (has OkHttp already). 5s timeout per ping. First launch briefly uses default URL — acceptable tradeoff.
**Status:** done

## 2026-05-30 — Fix appconfig double-quoting + exclude bitchat-mesh

**Goal:** Fix build failure caused by double-quoted buildConfig fields in appconfig; handle subsequent bitchat-mesh compilation errors

**Context:** Чатор fork. First build failure: `appconfig` generated `""чатор""` and `""https://matrix.org""` (Java String concatenation from properties with quotes). Second failure (after fix): `bitchat-mesh` — never compiled from source before due to prior build failing at appconfig + Gradle cache.

**Approach:**
- Fixed appconfig double-quoting in previous session (commit `ada69de`)
- Investigated new build failure: `bitchat-mesh` references packages `com.bitchat.lib.features.*`, `com.bitchat.lib.favorites.*`, `com.bitchat.lib.ui.*`, `com.bitchat.lib.sync.*` that don't exist in this repo
- Module also missing `kotlin-parcelize` plugin and `androidx.lifecycle:lifecycle-process` dep
- Excluded module from build (commented out `include` in settings.gradle.kts)

**Files created/modified:**
- `settings.gradle.kts` — commented out `include(":libraries:bitchat-mesh")`

**Key decisions:** Excluded rather than stubbed — module was written for full bitchat codebase, missing 4+ sibling modules. No other modules depend on it.

**Status:** done

## 2026-05-30 — Remove stale mesh files (fix CI build)

**Goal:** Fix CI build failure after bitchat-mesh exclusion
**Context:** Commit `c7d6e2bd` deleted `MeshMessageService.kt` but left `MeshScreen.kt` importing it, plus orphaned `NetworkConnectivityManager.kt`. CI builds failing consistently.
**Approach:**
- Deleted `app/.../features/mesh/MeshScreen.kt` (refs deleted `MeshMessageService`)
- Deleted `app/.../mesh/NetworkConnectivityManager.kt` (dead code, orphaned)
- Fixed two stale empty directories

**Files created/modified:**
- `app/.../features/mesh/MeshScreen.kt` — deleted
- `app/.../mesh/NetworkConnectivityManager.kt` — deleted

**Key decisions:** Clean wipe of all mesh leftovers. bitchat-mesh integration fully abandoned — module disabled, all stale files removed.

**Status:** fix pushed (93952964), CI build #361 in progress

## 2026-05-30 — Force single homeserver + full chator branding

**Goal:** Strip all homeserver choice from UI, force `chator.duckdns.com` only; full branding pass
**Context:** CI builds failing from stale mesh files. Once fixed, needed to lock homeserver and finalize branding.
**Approach:**
- `DefaultEnterpriseService.kt`: `defaultHomeserverList()` → `["https://chator.duckdns.com"]` only
- `DefaultEnterpriseServiceTest.kt`: updated expectations for single-item list
- Removed `matrix-client.matrix.org` from all configs
- Debug APK built and sitting at `chator-elementx-debug.zip` (240MB)
**Files created/modified:**
- `features/enterprise/impl-foss/.../DefaultEnterpriseService.kt`
- `features/enterprise/impl-foss/.../DefaultEnterpriseServiceTest.kt`
**Key decisions:** No UI changes to login screens — enterprise config alone forces single server. OnBoardingPresenter reads enterprise list and skips server selection automatically.
**Status:** done

## 2026-05-30 — Fix duckdns.org typo + delete stale .sites + CI build

**Goal:** Fix homeserver domain typo (duckdns.com → duckdns.org), remove leftover DPI bypass `.sites` file, verify fresh APK
**Context:** Commits `e6e08d2c` forced `duckdns.com` but config had `duckdns.org`. Also `proxytest_matrix.sites` was a DPI leftover still baked into old debug APK.
**Approach:**
- `DefaultEnterpriseService.kt`: `duckdns.com` → `duckdns.org`
- `DefaultEnterpriseServiceTest.kt`: fixed assertion to match
- `proxytest_matrix.sites`: deleted (was DPI bypass artifact, not needed for homeserver config)
- `chator-config.properties`: added with `CHATOR_HOMESERVER_URL=https://chator.duckdns.org`
- Verified old APK (`chator-elementx-debug.zip` 240MB) was stale — still had Element logos and old servers
- Diagnosed local Gradle failure: system Java 26 incompatible w/ Gradle 9.2.1 (needs ≤21). Android Studio JBR 21 works but local build takes 30min+
- **Decision: never run Gradle locally again** — use GitHub Actions CI only
- Commits pushed to `origin/develop`, CI auto-triggers on push
**Files created/modified:**
- `DefaultEnterpriseService.kt` (fix duckdns.org)
- `DefaultEnterpriseServiceTest.kt` (fix assertion)
- `proxytest_matrix.sites` (deleted)
- `chator-config.properties` (added)
**Status:** waiting for CI build on GitHub Actions
