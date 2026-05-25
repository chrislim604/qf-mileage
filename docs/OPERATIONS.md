# Operations

## Local Setup

Install:

- JDK 21 or newer.
- Gradle.
- Android SDK for full Android builds.
- Node.js for documentation and version scripts.

Useful commands:

```sh
npm run docs:check
npm run version:check
gradle :shared:allTests
```

Full Android builds require a configured Android SDK and signing setup for release builds.

On this Mac, the installed command-line SDK path is:

```text
/opt/homebrew/share/android-commandlinetools
```

Use it for local debug builds when `ANDROID_HOME` is not already set:

```sh
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools node scripts/run-gradle.mjs :app:assembleFreeDebug
```

## Secrets

Copy `.env.example` to `.env` for local-only configuration. Do not commit `.env`, API keys, billing IDs, ad unit IDs, OAuth credentials, or signing keys.

## Backups

The planned backup model is encrypted app-managed archives in Google Drive `appDataFolder`, plus Android Auto Backup for small settings/state. Restore must be tested before the backup flow is considered complete.

## Local Ledger Files

The v0.2.0 Android MVP writes:

- `qf-mileage-ledger.json` for private app ledger storage.
- `qf-mileage-logbook.csv` when the user exports the current ledger.

Both files are in Android private app storage during the debug MVP.

## Release Checklist

Use [Release Checklist](RELEASE_CHECKLIST.md) before commits or public pushes.
