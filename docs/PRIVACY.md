# Privacy

QF Mileage handles sensitive location history, vehicle, business-destination, advertising, billing, and backup data.

## Local-First Rule

Trip data stays on the device unless the user explicitly exports it, backs it up, or later configures ERPNext sync.

The v0.6.0 MVP stores manual trips, vehicle labels, job/client labels, and categories in private Android app storage. Appearance mode is stored in private Android preferences and defaults to Follow system. The app writes CSV or JSON backup files only when the user taps a share/export action.

## Location History

Google Maps Timeline data should be imported through a user-controlled workflow. The app must not assume a hidden public Timeline API or silently upload raw Timeline files.

## Google Drive Backup

MVP backup uses encrypted app-managed archives stored in the app's Google Drive `appDataFolder`. Generic Android Auto Backup may store small settings/state, but real trip archives should stay in the encrypted app-managed backup path.

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
