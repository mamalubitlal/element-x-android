# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About

**Чатор** — a fork of [Element X Android](https://github.com/element-hq/element-x-android) with Russian-language interface and custom branding. A Matrix-based messenger app for Android.

**Package:** `im.chator.android` | **Min SDK:** 24 (Android 7.0) | **Target SDK:** 36 | **JVM Target:** 21

## Key Commands

### Building
```bash
# Debug APK (what you need most of the time)
./gradlew :app:assembleGplayDebug

# Release APK (unsigned)
./gradlew :app:assembleGplayRelease

# Build with verbose output for debugging
./gradlew :app:assembleGplayDebug --stacktrace
```

### Testing
```bash
# Presenter tests (Molecule + Turbine)
./gradlew :features:<feature>:impl:test

# Paparazzi screenshot tests
./gradlew :features:<feature>:impl:verifyPaparazziDebug

# Kover code coverage
./gradlew :app:koverHtmlReport
./gradlew :app:koverVerify

# Konsist architectural tests
./gradlew :tests:konsist:test

# Maestro UI tests (see .maestro/README.md)
maestro test .maestro/flows/<flow>.yaml
```

### Linting
```bash
# Run all lint checks
./gradlew :app:lint

# Run Detekt static analysis
./gradlew detekt
```

### Native Code (ByeDPI)
The ByeDPI native library (DPI bypass) lives in `libraries/byedpi/`. Native C code is compiled via CMake.
```bash
# The library bundles its own CMakeLists.txt and builds libbyedpi.so
# JNI wrapper: libraries/byedpi/src/main/java/.../jni/ByeDpiJni.kt
# Kotlin server: libraries/byedpi/src/main/java/.../server/ByeDpiServer.kt
```

## Architecture

### Module Structure
Multi-module Gradle project with four main module types:

- **`app`** — Main Android application module. Entry point, MainActivity, DI graph setup.
- **`appnav`** — Navigation glue, holds RootNode.
- **`features/`** — UI screens/flows. Each feature has `api` (interfaces, data classes) and `impl` (presenter, view, DI modules) submodules.
- **`libraries/`** — Shared utility and domain code. Also has `api`/`impl` split.
- **`services/`** — Background services and long-running processes.

Dependency rule: features should not depend on each other directly. Navigation is handled in `appnav` and `app`.

### Key Patterns

**Presenter/View/State architecture** (inspired by Circuit):
- `Presenter` — Compose function returning `State`. Uses Molecule for reactive state management.
- `View` — Compose UI that takes `State` and emits `Event`s.
- `State`/`Event` — Communication channel between Presenter and View (never communicate directly).
- `Node` — Connection point between View and Presenter; manages DI graphs.
- Single Activity app (`MainActivity`) holds and configures the `RootNode`.
- No ViewModel needed — configuration changes handled by Compose runtime.

**Dependency Injection:** Metro (https://zacsweers.github.io/metro/latest/)
- DI graphs are defined in `di/` packages within modules.
- Child nodes can create sub-graphs from parent nodes.

**Navigation:** Appyx (https://bumble-tech.github.io/appyx/)
- Navigation state managed through Appyx models.

**Matrix SDK:** Rust SDK with Kotlin bindings via Uniffi
- `libraries/matrix/` wraps the Matrix Rust SDK for Kotlin consumption.
- SDK published as `org.matrix.rustcomponents:sdk-android` on Maven.
- To build SDK locally: `tools/sdk/build-rust-sdk` (requires cargo-ndk, NDK).

### ByeDPI Architecture (custom addition)
- **`libraries/byedpi/`** — The DPI bypass library module. Contains:
  - Native C code (`src/main/cpp/native-jni.c`) compiled to `libbyedpi.so` via CMake. Uses `server_fd` global for shutdown control.
  - `ByeDpiJni` — JNI wrapper (static native methods `startNativeProxy`/`stopNativeProxy`, loads the .so)
  - `ByeDpiServer.kt` — Pure Kotlin server manager using `ProxyController` interface
  - `ByeDpiLibrary.kt` — Main API entry point. Wires `JniProxyController` (object implementing `ProxyController`) to the server on init
  - Consumed by `libraries/dpi/impl/` via `projects.libraries.byedpi` dependency
- Usage: `ByeDpiLibrary` → `ByeDpiServer` → `ProxyController` → `ByeDpiJni` → native `.so`
- APK packaging: `jniLibs.pickFirsts += "**/libbyedpi.so"` handles duplicate .so conflicts (see `app/build.gradle.kts`)

## Important Notes

- **Gradle version catalog** — All dependencies in `gradle/libs.versions.toml`. Renovate auto-updates deps.
- **Logging** — Use Timber, NEVER log private user data. Use `Timber.tag(loggerTag).d()` format.
- **No mockk** — Prefer Fake implementations over mocking for interfaces. Mockk only for Android framework classes.
- **Naming conventions** — Presenters MUST end in `Presenter`, States in `State`, etc. Required for code coverage rules.
- **Translations** — Managed via Localazy. See `tools/localazy/README.md`.
- **Cache issues** — If Gradle cache misbehaves: add `--no-build-cache` to the gradlew command.
- **Building from Android Studio** — Open project root, select `app` configuration, build.
- **CI workflow** — `.github/workflows/chator-build.yml` builds Debug APK on push to `develop`/`main`/`master`.
