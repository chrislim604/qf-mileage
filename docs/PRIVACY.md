# Privacy

QF Mileage handles sensitive location history, vehicle, business-destination, advertising, billing, and backup data.

## Local-First Rule

Trip data stays on the device unless the user explicitly exports it, backs it up, or later configures ERPNext sync.

The v0.9.0 MVP stores manual trips, imported trip summaries, vehicle labels, saved place labels, job/client labels, and categories in private Android app storage. Appearance mode is stored in private Android preferences and defaults to Follow system. The app writes CSV, debug JSON backup, or encrypted backup files only when the user taps a share/export action.

## Location History

Google Maps Timeline data is imported through a user-controlled Android document picker. The app reads only the selected JSON file, normalizes supported driving segments into review trips, and does not upload raw Timeline files. The app must not assume a hidden public Timeline API or silently collect Timeline history in the background.

The app may check whether Android device Location providers are enabled so it can guide setup. It does not read the user's Google account Timeline status directly because Google does not expose that as a public third-party app API. Enabling or confirming Timeline remains a user action in Google settings.

## Saved Places

Saved places can reveal home, office, warehouse, supplier, and client patterns. They remain local ledger data unless the user explicitly exports or backs up the ledger. Frequent endpoint suggestions must be approved by the user before they are stored as saved places.

## Google Drive Backup

MVP backup uses encrypted app-managed archives stored in the app's Google Drive `appDataFolder`. The Android debug app creates `.qfmbackup` archives with AES-GCM encryption using an Android Keystore key before any future Drive upload. Generic Android Auto Backup may store small settings/state, but raw ledgers and real trip archives are excluded from generic Auto Backup.

## Advertising

The free app may show banner ads during normal operation and interstitial ads at review/export transitions. Ads must not interrupt editing, permission prompts, error recovery, billing, or sensitive review decisions.

## Billing

The free app uses Google Play Billing for the remove-ads purchase. The paid app should not show ad UI or remove-ads prompts.

## Public Repository Rules

Never commit:

- real Timeline exports;
- GPS traces;
- home/client addresses;
- API keys;
- billing product IDs;
- ad unit IDs;
- OAuth credentials;
- keystores;
- private backups or exports.

Use synthetic fixtures only.
