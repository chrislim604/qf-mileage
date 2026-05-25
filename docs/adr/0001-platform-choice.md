# ADR 0001: Kotlin Multiplatform Shared Core With Native Android UI

## Status

Accepted.

## Decision

Use Kotlin Multiplatform for shared domain logic and a native Android UI for v1. Future iOS should use a native SwiftUI shell over the shared core. Do not use shared Compose UI for iOS in v1.

## Context

QF Mileage is platform-heavy. It needs location evidence import, route reconstruction, Bluetooth vehicle hints, background-conscious trip assist, Google Drive backup, app-store billing, ads, and future iOS equivalents.

## Considered Options

- Flutter: strong cross-platform UI, but the sensitive edges still need native platform API work.
- React Native: viable, but native-module complexity would sit on the core privacy and background behavior.
- Capacitor: good for web-forward apps, weaker for this native/background/store-integration profile.
- Kotlin Multiplatform: shares domain logic while allowing native UI and native adapters on each platform.

## Consequences

- Shared code must stay platform-neutral.
- Android launches first with Jetpack Compose.
- iOS can later use SwiftUI, MapKit, iCloud/CloudKit, and StoreKit without inheriting Android UI assumptions.
- Platform adapter contracts need to be explicit from the start.
