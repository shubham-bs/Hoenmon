# HomeEnergyMonitor — Agent Guide

Android smart-home electricity monitor (Kotlin/Compose, MVVM) + Java 21 / Spring Boot
backend + PostgreSQL. v1 uses simulated telemetry only. Modular monolith.

## Workspace rules

- Project root: `BTP/HomeEnergyMonitor/`. BTP is a shared workspace: NEVER modify,
  delete, rename, or create files outside `HomeEnergyMonitor/`.
- Two independent Gradle builds, each with its own wrapper:
  - `android/` → Gradle 8.11.1, AGP 8.10.1, Kotlin 2.1.21, Compose BOM 2025.06.01
  - `backend/` → Gradle 9.6.0, Spring Boot 4.1.0, Java 21
- Always run `./gradlew` from inside the relevant build directory. Do NOT use the
  globally installed `gradle` (9.7.1) for builds — it is not compatible with AGP 8.10.1.

## Verified environment (this machine)

- JDK: `java`/`javac` = 21.0.11 (JAVA_HOME → Microsoft OpenJDK 21). JDK 26 is ALSO
  installed — never let the backend compile on 26; it is pinned via Gradle toolchain 21.
- Android SDK: `/Users/saymyname/Library/Android/sdk` (ANDROID_HOME is UNSET; it is set
  in `android/local.properties` via `sdk.dir`).
- Installed: platforms android-36 / android-37.0, build-tools 35.0.0 / 36.0.0,
  AVD `Pixel_9_Pro`, system image android-37.2-beta3 (arm64).
- `adb` and `emulator` are NOT on PATH — use full paths under
  `.../sdk/platform-tools/adb` and `.../sdk/emulator/emulator`.
- cmdline-tools / sdkmanager are NOT installed — manage SDKs through Android Studio.
- PostgreSQL runs via `docker compose`; local `psql` is not installed.
- compileSdk is 36; AGP 8.10.1 does not support the installed android-37 platform.

## Commands

- DB up:            `docker compose up -d`                  (from project root)
- DB health:        `docker compose ps`
- Backend run:      `./gradlew bootRun`                     (in backend/, port 8080)
- Backend build:    `./gradlew build`
- Backend test:     `./gradlew test`
- One backend test: `./gradlew test --tests "com.homeenergymonitor.<Class>"`
- Android build:    `./gradlew :app:assembleDebug`          (in android/)
- Android test:     `./gradlew :app:testDebugUnitTest`
- One Android test: `./gradlew :app:testDebugUnitTest --tests "<Class>"`
- Install APK:      `./gradlew :app:installDebug`

## Architecture rules

- Hardware boundary = `MeterAdapter` interface (backend `meter/`). v1 ships ONLY a
  simulated adapter. Never hardcode a specific meter manufacturer/model.
- Live updates: backend pushes over SSE (in-memory `SseEmitter` registry). No Kafka,
  no Redis, no microservices.
- Backend is a modular monolith (package-by-feature): meter / telemetry / analytics /
  web / persistence / config.
- Android is MVVM: `ui/` (Compose + ViewModel StateFlow) / `domain/` / `data/`
  (OkHttp SSE + REST, Room cache).
- The Android emulator reaches the host backend at `10.0.2.2:8080`, NOT `localhost`.

## Product rules

- ALL simulated readings must be visibly labeled `DEMO DATA` in the UI.
- Real hardware integration is a later milestone — never present simulated data as real.
