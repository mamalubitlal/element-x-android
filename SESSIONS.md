## 2026-06-20 — In-app account registration via password screen

**Goal:** Allow users to create new accounts directly in-app when the server doesn't support OIDC/OAuth registration or the OIDC prompt=create fails.

**Context:** Server supports password login but not OIDC account creation. The OIDC provider doesn't support `prompt=create`, so the registration flow in `LoginHelper` fell through to a web-based account creation URL, which also throws `AccountCreationNotSupported` for servers without a web registration page.

**Approach:**
- `RegistrationApi.kt` — new HTTP client that POSTs to `/_matrix/client/v3/register` with `m.login.dummy` auth, handles session-based multi-step registration per Matrix spec.
- `LoginMode.PasswordLogin` changed from object to data class with `isAccountCreation: Boolean` flag.
- Callback chain: `LoginFlowNode` → `LoginPasswordNode.Inputs` → `LoginPasswordPresenter` — all updated to pass `isAccountCreation`.
- `LoginPasswordPresenter.submit()` branches: when `isAccountCreation=true`, calls `RegistrationApi.register()` first, then `authenticationService.login()` with the same creds.
- `LoginPasswordState` exposes `isAccountCreation` for future UI differentiation.
- `LoginHelper.submit()` catches `AuthenticationException.OAuth` from OIDC `prompt=create` failure and falls back to `PasswordLogin(isAccountCreation=true)` when password login is available.
- `ClassicFlowNode` callback updated for `isAccountCreation` passthrough.

**Files created/modified:**
- `features/login/impl/.../loginpassword/RegistrationApi.kt` (new)
- `features/login/impl/.../login/LoginMode.kt`
- `features/login/impl/.../login/LoginHelper.kt`
- `features/login/impl/.../login/LoginModeView.kt`
- `features/login/impl/.../loginpassword/LoginPasswordPresenter.kt`
- `features/login/impl/.../loginpassword/LoginPasswordState.kt`
- `features/login/impl/.../loginpassword/LoginPasswordNode.kt`
- `features/login/impl/.../loginpassword/LoginPasswordStateProvider.kt`
- `features/login/impl/.../LoginFlowNode.kt`
- `features/login/impl/.../classic/ClassicFlowNode.kt`
- `features/login/impl/.../classic/loginwithclassic/LoginWithClassicNode.kt`
- `features/login/impl/.../classic/loginwithclassic/LoginWithClassicView.kt`
- `features/login/impl/.../chooseaccountprovider/ChooseAccountProviderNode.kt`
- `features/login/impl/.../chooseaccountprovider/ChooseAccountProviderView.kt`
- `features/login/impl/.../confirmaccountprovider/ConfirmAccountProviderNode.kt`
- `features/login/impl/.../confirmaccountprovider/ConfirmAccountProviderView.kt`
- `features/login/impl/.../onboarding/OnBoardingNode.kt`
- `features/login/impl/.../onboarding/OnBoardingView.kt`

**Key decisions:**
- `RegistrationApi` uses raw `HttpURLConnection` (no OkHttp dependency needed) + `kotlinx.serialization.json` (already a dep in the module).
- Registration succeeds → login again with same creds to get a session via the SDK (since `RegistrationApi` gets raw Matrix tokens, not SDK session objects).
- Two-step registration: first request with `m.login.dummy`, if server returns a `session`, retry with the session in auth.
- `AuthenticationException.OAuth` caught specifically for OIDC prompt failures (not all exceptions) to avoid masking real errors.

**Status:** Done — builds & APK assembled via GH Actions. Needs real-device test.

## 2026-06-20 — Fix login failure: `setHomeserver()` never called in FOSS flow

**Goal:** Fix the "generic error" when user taps "Continue" → enters credentials on password login screen.

**Context:** APK b51b939f (commit dedae976f8) with the protocol-stripping fix installed on Xiaomi 23108RN04Y. Auth metadata endpoint works. But login still fails with generic error.

**Root cause:** In the FOSS build (no enterprise config), `defaultAccountProvider` is null and `canConnectToAnyHomeserver` is true. Tapping "Continue" on OnBoarding called `onSignIn(false)` → `navigateToSignInFlow(false)` → directly pushed `LoginPassword`. This bypassed `LoginHelper.submit()` entirely, meaning `setHomeserver()` was never called before `authenticationService.login()`, causing `IllegalStateException("You need to call setHomeserver() first")` mapped to a generic error.

**Fix (commit TBD):**
- `OnBoardingState.kt`: Added `accountProviderUrl: String` field. Simplified `submitEnabled` (removed `defaultAccountProvider != null` guard since the null case now also needs it).
- `OnBoardingPresenter.kt`: Now collects `accountProvider` from `AccountProviderDataSource.flow` and passes `accountProvider.url` to state.
- `OnBoardingView.kt`: When `defaultAccountProvider` is null and `mustChooseAccountProvider` is false, the "Continue" button now calls `state.eventSink(OnBoardingEvents.OnSignIn(state.accountProviderUrl))` instead of `onSignIn(false)`. This routes through `LoginHelper.submit()` which calls `setHomeserver()`, detects OAuth support, and returns the correct `LoginMode` (OAuth → browser, PasswordLogin → password screen). The `mustChooseAccountProvider = true` case is preserved (navigates to ChooseAccountProvider).
- `OnBoardingStateProvider.kt`: Added `accountProviderUrl` parameter with default.

**Expected result:** User taps "Continue" → `LoginHelper.submit()` calls `setHomeserver("https://chator.crabdance.com")` → detects OAuth support → `LoginModeView` triggers `onOAuthDetails` → opens browser for OAuth flow.

**Files modified:**
- `features/login/impl/src/main/.../screens/onboarding/OnBoardingPresenter.kt`
- `features/login/impl/src/main/.../screens/onboarding/OnBoardingState.kt`
- `features/login/impl/src/main/.../screens/onboarding/OnBoardingStateProvider.kt`
- `features/login/impl/src/main/.../screens/onboarding/OnBoardingView.kt`

**Status:** Done — needs APK build and real-device test.

## 2026-06-18 — MAS error actually server-side: missing auth_metadata endpoint

**Goal:** Fix "no MAS" error still appearing after the protocol-stripping fix.

**Context:** APK with the fix was installed on BlueStacks and tested. Logs reveal the Kotlin fix works (well-known is resolved), but the Rust SDK hits a 404 on `/_matrix/client/unstable/org.matrix.msc2965/auth_metadata` and reports OAuth unsupported.

**Root cause (real):** The server's nginx doesn't serve `/_matrix/client/unstable/org.matrix.msc2965/auth_metadata` — the Rust SDK uses this endpoint to verify OAuth support, not OIDC issuer discovery from `.well-known`. The endpoint returns 404, so `supportsOauthLogin()` returns `false`.

**What works:** Password login (`m.login.password`) works fine — user `@uggan:chator.duckdns.org` logged in successfully.

**Fix needed (server-side):** Configure nginx to proxy `/_matrix/client/unstable/org.matrix.msc2965/auth_metadata` to the MAS backend. This endpoint should return OAuth metadata (same info as `/.well-known/openid-configuration`).

**Status:** blocked on server config

## 2026-06-18 — Strip protocol prefix from serverName in login & compatibility check

## 2026-06-17 — Fix OIDC login + restore AvatarDataFetcherFactory cast

**Goal:** Fix account creation/login failing because OIDC issuer not discovered from `.well-known/matrix/client`.

**Context:** Account creation (and OIDC login) was broken because `setHomeserver(homeserverUrl)` was called with a full URL (with protocol prefix), causing the Rust Matrix SDK to skip well-known resolution. The OIDC issuer was never discovered.

**Root cause:** `serverNameOrHomeserverUrl(url)` with a full URL (`https://chator.crabdance.com`) — SDK interprets it as a direct homeserver URL and skips `.well-known` discovery. Fix: `serverName()` strips the protocol, forcing well-known resolution.

**Approach:**
- Changed `RustMatrixAuthenticationService.setHomeserver()`: `serverNameOrHomeserverUrl(url)` → `serverName()`
- Changed `RustHomeServerLoginCompatibilityChecker.check()`: same fix for user-typed custom servers
- Accidentally removed `as? Fetcher` cast in `AvatarDataFetcherFactory.kt` (Coil 2.6.0's `newFetcher` returns `Any?`) — restored it after CI build #2 failed

**Files modified:**
- `libraries/matrix/impl/src/main/kotlin/io/element/android/libraries/matrix/impl/auth/RustMatrixAuthenticationService.kt` — serverName fix
- `libraries/matrixui/impl/src/main/kotlin/io/element/android/libraries/matrix/ui/check/RustHomeServerLoginCompatibilityChecker.kt` — serverName fix
- `libraries/matrixmedia/impl/src/main/kotlin/io/element/android/libraries/matrix/ui/media/AvatarDataFetcherFactory.kt` — restored `as? Fetcher` cast

**CI results:**
| Build | Commit | Result |
|-------|--------|--------|
| чатор Android Build #1 | serverName fix only | ✅ |
| чатор Android Build #2 | + removed cast | ❌ |
| чатор Android Build #3 | + restored cast | ✅ |

**Status:** done — 3 CI builds, all fixed. APK downloading.
