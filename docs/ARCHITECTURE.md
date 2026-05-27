# Architecture

QF Mileage is Android-first, iOS-ready, and local-first.

## Modules

- `shared`: Kotlin Multiplatform core for mileage domain logic, CRA review rules, route-provider contracts, backup contracts, entitlement state, and ERPNext export contracts.
- `app`: Native Android shell using Jetpack Compose, a file-backed local ledger store, and platform adapters.

The shared core must not depend on Android, Google Play, AdMob, Google Drive, Bluetooth, Activity Recognition, or ERPNext SDKs directly.

## Platform Adapters

Android adapters planned after the scaffold:

- File-backed local ledger store for the v0.2.0 MVP.
- Google Maps Timeline import parser.
- Google Routes distance provider.
- Google Drive `appDataFolder` encrypted backup repository.
- Android Auto Backup settings integration.
- Bluetooth connection observer for vehicle hints.
- Activity Recognition trip assist.
- AdMob ad placement controller.
- Google Play Billing entitlement repository.
- ERPNext export/sync adapter.

Future iOS adapters:

- SwiftUI app shell.
- MapKit distance provider.
- iCloud/CloudKit backup adapter.
- StoreKit entitlement adapter.

## Data Flow

1. Import or enter trip evidence.
2. Reconstruct driven distance when source data has gaps or jumps.
3. Attach vehicle and place suggestions.
4. Put trip into the review queue.
5. User approves purpose, treatment, notes, and claimable distance.
6. Export CRA-style evidence, create encrypted backup, or sync to ERPNext later.

The v0.7.0 implementation supports a tabbed Android workflow, Follow system/Light/Dark appearance settings, explicit date/time manual entry, local persistence, vehicle deletion, date-range review filtering, job/client and category classification, trip deletion, local CSV export sharing, debug JSON backup sharing, Android Keystore encrypted backup archive sharing, and latest encrypted backup restore. Timeline import, route reconstruction, Drive upload/download, ads, billing, and ERPNext sync remain adapter work after this vertical slice.

## Privacy Boundary

The phone is the source of truth. Trip evidence remains local unless the user explicitly exports, backs up, or later configures ERPNext sync.
