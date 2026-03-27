# Fix Plan — чатор Android Integration

## TODO (Priority Order)

### Phase 1: Login/Register Flow (Element Web Style, OIDC-LESS)

**Reference:** Element Web source (`Login.ts`, `Login.tsx`, `Registration.tsx`)
**Spec:** `specs/01-oidc-less-auth.md`

1. [x] **Create MatrixAuthService** — core auth logic ✅ DONE
   - `login(username, password)` → EXISTS in RustMatrixAuthenticationService (via Rust SDK)
   - `register(username, password, email)` → EXISTS in RustMatrixAuthenticationService (via HTTP)
   - `getLoginFlows()` → Implemented via setHomeserver() returning MatrixHomeServerDetails.supportsPasswordLogin
   - Token storage → Implemented via SessionStore (encrypted database, not EncryptedSharedPreferences)
   - Note: The auth service was already implemented in the Element X fork

2. [ ] **Create LoginScreen** — Compose UI
   - Username + password fields
   - Login button
   - "Create Account" link → navigate to registration
   - Russian localization
   - Error handling (wrong credentials, network errors)

3. [ ] **Create RegistrationScreen** — Compose UI
   - Username + password + confirm + email fields
   - Create Account button
   - Handle UIA stages (email verification)
   - Russian localization
   - Auto-login on success

4. [ ] **Remove OIDC/SSO complexity** — strip from existing code
   - Remove Dex OIDC references
   - Remove SSO buttons from login
   - Simplify auth flow to password-only

5. [ ] **Update navigation** — login → register → main app
   - Simple flow: no server picker, no SSO
   - Direct to chat list after auth
   - Pre-configured server: chator.k.vu

### Phase 2: ByeDPI Integration

**Spec:** `specs/02-dpi-bypass.md`
**Reference:** `../chator-dpi-tester/` (working implementation)

5. [ ] **Copy ByeDPI core files** to Element X structure
   - DpiStrategyManager.kt (strategy storage & apply)
   - NetworkChangeObserver.kt (WiFi/Mobile detection)
   - DpiAutoTestWorker.kt (background test worker)
   - MatrixStrategyTester.kt (core test logic)
   - SiteCheckUtils.kt (HTTP connectivity test)
   - StrategyResult.kt (data classes)

6. [ ] **Copy assets** — strategies and test domains
   - proxytest_strategies.list (71 strategies)
   - proxytest_matrix.sites (8 Matrix domains)

7. [ ] **Add dependencies** to build.gradle.kts
   - WorkManager: `androidx.work:work-runtime-ktx:2.9.0`
   - Gson: `com.google.code.gson:gson:2.10.1`

8. [ ] **Update ElementXApplication.kt** — first-boot detection
   - Initialize DpiStrategyManager
   - Start NetworkChangeObserver
   - Schedule first-boot test (71 strategies, ~5 min)
   - Handle network change events

9. [ ] **Add permissions** to AndroidManifest.xml
   - ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE
   - ACCESS_WIFI_STATE, INTERNET

10. [ ] **Add strings** — Russian localization
    - 15+ DPI-related strings in values-ru/strings.xml

### Phase 3: Strategy Picker UI

**Spec:** `specs/03-strategy-picker.md`

11. [ ] **Create StrategyPickerView** — Compose UI
    - LazyColumn with 71 strategies
    - Show success rate, last tested, active indicator
    - Tap to select & apply

12. [ ] **Create StrategyTestView** — progress UI
    - Progress bar (X of 71 strategies)
    - Current strategy + domain results
    - Cancel button
    - ETA display

13. [ ] **Create StrategyPickerViewModel** — state management
    - Load strategies with stats
    - Select/apply strategy
    - Start manual test

14. [ ] **Add Settings integration** — DPI Bypass category
    - Current strategy display
    - Auto-test toggle
    - "Test strategies now" button → navigate to picker

15. [ ] **Add Bug Report integration** — quick retest
    - "Re-test DPI strategies" button
    - Useful for connection issues

### Phase 4: Polish & Testing
15. [ ] **Add notifications** — test completion
    - "DPI test complete" notification
    - Tap to view results
    - Dismissible

16. [ ] **Add strategy expiry** — re-test after 24h
    - Check timestamp on strategy load
    - Auto-schedule re-test if stale

17. [ ] **Test all flows** — manual QA
    - First-boot test runs
    - Network change detection
    - Manual test from Settings
    - Manual test from Bug Report
    - Strategy save/load
    - Auto-apply on network switch

18. [ ] **Performance optimization** — reduce test time
    - Parallel domain testing
    - Smart strategy skip (obviously bad ones)
    - Timeout per test (don't hang)

19. [ ] **Error handling** — graceful failures
    - Network unavailable → retry later
    - Test failed → try next strategy
    - No working strategy → show error

20. [ ] **Build verification** — full CI/CD
    - Debug APK builds
    - Release APK builds (fix signing)
    - Tests pass
    - Lint clean

## Completed

- [x] Ralph Loop structure created
- [x] PROMPT.md written
- [x] AGENT.md written
- [x] fix_plan.md seeded

## In Progress

- Item #1: MatrixAuthService - COMPLETE (already existed in codebase)

## Discovered Issues

- Issue #1: Server chator-server.onrender.com DOWN since ~04:00 UTC (needs manual wake)
- Issue #2: Build #68 failed on Release APK signing (Debug APK works)
- Issue #3: OIDC complexity in current code — needs simplification
- Issue #4: Build requires Java 21, but only Java 17 is available in environment

## Notes

- Iteration 0: Ralph Loop setup complete
- Server needs manual wake on Render dashboard
- Debug APK available from build #68 for testing
- Focus on simplicity: login/register → DPI bypass → strategy picker

## Iteration 1 Summary (2026-03-27)

- Analyzed fix_plan.md item #1: MatrixAuthService
- DISCOVERY: MatrixAuthenticationService interface already exists in codebase
- Implementation: RustMatrixAuthenticationService class provides:
  - login(username, password) via Rust SDK
  - register(username, password) via direct HTTP call
  - getLoginFlows() via setHomeserver() returning MatrixHomeServerDetails
  - Token storage via SessionStore (encrypted database)
- CONCLUSION: Item #1 is already complete - no new code needed
- NEXT: Move to item #2 (Create LoginScreen) or item #4 (Remove OIDC complexity)
