# Hoenmon

Native Android app for monitoring power consumption, backed by a Spring Boot service and PostgreSQL.

**Version 1 uses simulated telemetry.** Every simulated reading is labeled **DEMO DATA** and must not be treated as a real measurement.

## Repository layout

```text
Hoenmon/
├── android/            Android app (Kotlin, Jetpack Compose, Material 3, MVVM)
├── backend/            Spring Boot service (Java 21, Gradle)
├── docs/               Architecture notes
├── docker-compose.yml  Local PostgreSQL
└── AGENTS.md           Development environment notes
```

## Prerequisites

- JDK 21
- Android Studio and Android SDK
- Docker Desktop with Docker Compose
- Android emulator or a physical Android device

See `AGENTS.md` for project-specific environment details and `docs/architecture.md` for the architecture.

## Run locally

### 1. Start PostgreSQL

From the repository root:

```bash
docker compose up -d
docker compose ps
```

### 2. Start the backend

In a separate terminal:

```bash
cd backend
./gradlew build
./gradlew bootRun
```

The backend runs at:

```text
http://localhost:8080
```

Check the current meter endpoint:

```bash
curl http://localhost:8080/api/v1/meter/current
```

The response contains simulated meter telemetry. The source is `SIMULATED` and the data is marked as demo data.

### 3. Run the Android app

1. Open the `android/` directory in Android Studio.
2. Wait for Gradle sync to complete.
3. Start an emulator from Device Manager, or connect a physical Android device.
4. Select the `app` run configuration and click **Run**.

For the Android emulator, configure the backend base URL as:

```text
http://10.0.2.2:8080/
```

`10.0.2.2` is the emulator's route to the host computer. For a physical device, use your computer's LAN IP address instead (for example, `http://192.168.x.x:8080/`) and ensure both devices are on the same network.

Keep PostgreSQL and the backend running while using the app.

## Build a debug APK

From the repository root:

```bash
cd android
./gradlew :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch the app:

```bash
adb shell monkey -p com.homeenergymonitor 1
```

## Main backend endpoints

```text
GET /api/v1/meter/current
GET /api/v1/telemetry/stream
GET /api/v1/meter/*
```

The live telemetry endpoint uses Server-Sent Events (SSE).

## Current scope

- Simulated single-phase household meter
- Live telemetry, historical readings and analytics in the Android app
- PostgreSQL-backed local backend setup
- No physical smart-meter integration yet

All displayed telemetry is simulated **DEMO DATA**.
