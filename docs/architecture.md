# HomeEnergyMonitor — Architecture

Android smart-home electricity monitor with a Spring Boot backend and PostgreSQL.
Version 1 is hardware-independent and runs on simulated telemetry.

## Components

```
Android app (Kotlin / Compose / MVVM)            Backend (Java 21 / Spring Boot)
  ui/      Compose screens + ViewModels  <---->  web/          REST + SSE controllers
  domain/  models + use cases                    telemetry/    ingest + SSE fan-out
  data/    SSE/REST client + Room cache          meter/        MeterAdapter abstraction
                                                 analytics/    daily/monthly aggregates
                                                 persistence/  JPA + Flyway
                                                         |
                                                    PostgreSQL
```

## Backend — modular monolith

Single deployable Spring Boot application, organized package-by-feature
(`meter`, `telemetry`, `analytics`, `web`, `persistence`, `config`). No
microservices, no Kafka, no Redis.

### MeterAdapter abstraction

`MeterAdapter` is the only hardware boundary. It exposes connect / subscribe /
status and emits readings into `telemetry`. Version 1 provides a simulated
adapter only; real adapters (e.g. Modbus, MQTT gateway) are added later without
changing telemetry or web code.

### Live data flow

1. A scheduler ticks the active `MeterAdapter`.
2. `telemetry` persists each reading to PostgreSQL and fans it out to connected
   clients through an in-memory `SseEmitter` registry.
3. REST endpoints serve latest readings, meter status, and historical analytics;
   SSE serves the live stream.

In-memory fan-out is acceptable for a single-instance modular monolith.

## Android — MVVM

- `ui/` — Jetpack Compose (Material 3) screens + ViewModels exposing `StateFlow`.
- `domain/` — plain models and use cases.
- `data/` — OkHttp SSE + REST client and Room cache behind repositories.

The app receives live readings over SSE, caches the latest values in Room, and
renders them. The emulator reaches the host backend at `10.0.2.2:8080`.

## Product rules

- Every simulated reading is labeled **DEMO DATA** in the UI.
- Real hardware is a later milestone; simulated data must never be presented as real.

## Milestones

- **M0 — bootstrap (done):** project skeleton, pinned Gradle wrappers, PostgreSQL
  service, build verification.
- **M1 — walking skeleton:** simulated `MeterAdapter` → SSE → Android displays live
  power + connection status with the DEMO DATA banner; latest reading cached in Room.
- **M2+:** live charts, historical analytics, real hardware adapter.
