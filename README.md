# QF Mileage

QF Mileage is an Android-first, iOS-ready mileage ledger for QuantumForm work travel.

The launcher name is intentionally short: `QF Mileage`. The planned public store names are:

- `QuantumForm Mileage` for the free app with ads.
- `QuantumForm Mileage Ad-Free` for the paid up-front version.

## Current Status

Version `0.8.0` is the review-first Timeline import MVP. It establishes:

- Kotlin Multiplatform shared core for mileage rules and future portability.
- Native Android app shell with free and paid product flavors.
- CRA-style review decision logic.
- Backup, route provider, and ERPNext export contracts.
- Public-repo documentation, versioning, GitHub templates, and release hygiene.
- File-backed on-device trip ledger storage.
- Android tab screens for trips, review, import, vehicles, export/backup, and settings.
- Appearance settings for Follow system, Light, and Dark themes, with Follow system as the default.
- QuantumForm brand colors: Accent Gold `#C58A2F`, Primary Black `#111111`, and White `#FFFFFF`.
- Android Keystore encrypted `.qfmbackup` archive creation and latest-backup restore.
- Manual trip entry by date, start time, end time, start point, and end point.
- User-selected Google Timeline/Takeout-style JSON import for driving segments.
- Imported trips start in the review queue and de-duplicate across repeated imports.
- Vehicle creation and deletion, with deleted vehicle references cleared from existing trips instead of deleting trip history.
- Review actions for classifying pending trips as business or personal with job/client and category labels.
- Trip deletion from review and ledger views.
- Android share-sheet export for CSV logbooks and JSON backups.
- Corrupt local ledger recovery by preserving the bad file and starting clean.

The app is not store-ready yet. It does not yet connect to live Google Routes, Google Drive, AdMob, Google Play Billing, or ERPNext.

## Product Direction

MVP capabilities:

- Manual trip entry by date, time, start point, end point, vehicle, job/client, and category.
- Local trip and vehicle persistence.
- Local CRA-style CSV export and JSON backup sharing.
- Google Maps Timeline-assisted import and review.
- Google Routes driven-distance reconstruction from start/end/waypoints.
- Multiple vehicles.
- Bluetooth vehicle hints from observed connection events after install.
- Approved frequent-place tags such as Home, Office, Warehouse, Client, Supplier, Personal, and Custom.
- Daily review reminders by default, with configurable cadence.
- CRA-style evidence exports.
- Encrypted app-managed Google Drive backup.
- ERPNext-ready export contracts for later integration.

## Architecture

The platform decision is Kotlin Multiplatform shared core, native Android UI first, and a future native SwiftUI iOS shell. Shared Compose UI for iOS is intentionally out of scope for v1.

See:

- [Architecture](docs/ARCHITECTURE.md)
- [Data Model](docs/DATA_MODEL.md)
- [Privacy](docs/PRIVACY.md)
- [Roadmap](docs/ROADMAP.md)
- [Platform ADR](docs/adr/0001-platform-choice.md)

## Development

Required local tools:

- JDK 21 or newer.
- Gradle.
- Android SDK for full Android builds.
- Node.js for release/version/documentation scripts.

Common checks:

```sh
npm run docs:check
npm run version:check
npm test
```

Full Android builds require a configured Android SDK. On this Mac, Homebrew's command-line SDK is installed at `/opt/homebrew/share/android-commandlinetools`, so a local Android build can be run with:

```sh
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools node scripts/run-gradle.mjs :app:assembleFreeDebug
```

## Local Ledger MVP

The Android app currently persists data to `qf-mileage-ledger.json` in private app storage. CSV exports are written to `qf-mileage-logbook.csv`, debug JSON backups are written to `qf-mileage-backup.json`, and encrypted backup archives are written to `qf-mileage-backup.qfmbackup`. Backup and export files can be shared through Android's share sheet. Appearance settings are stored in private Android preferences and default to Follow system. The main app is organized into tabs for trips, review, import, vehicles, export/backup, and settings. Review can be narrowed to a target date range, and approved trips can carry job/client and category labels for later ERPNext mapping. Timeline JSON imports are user-selected, normalized into review trips, and de-duplicated by stable import IDs. This keeps the first MVP simple and local-first while preserving the future path to SQLDelight/Room, encrypted Drive `appDataFolder` sync, and ERPNext sync.

## Versioning

Every completed change must include at least a patch version bump. Feature/default-visible behavior changes are normally minor. Breaking data/export changes and store/milestone releases are major.

See [Versioning](docs/VERSIONING.md).

## Repository Policy

This repository is public and source-available, but it intentionally has no open-source license at this stage. No reuse rights are granted until a license is deliberately added.

Never commit:

- API keys, billing product IDs, ad unit IDs, OAuth credentials, or keystores.
- Real Timeline exports, trip history, addresses, or location traces.
- Private app backups, exports, screenshots, or database files.

## Release

The release gate is:

```sh
npm run docs:check
npm run version:check
npm test
npm run secrets:scan
```

After verification, commit locally, push to GitHub, capture the closeout to shared memory, and re-index memory.
