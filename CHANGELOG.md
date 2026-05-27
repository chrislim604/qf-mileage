# Changelog

All notable changes are tracked here. Versioning follows SemVer and is part of the standard workflow.

## 0.1.0 - 2026-05-25

- minor: establish the QF Mileage initial scaffold.
- detail: Adds Kotlin Multiplatform shared core, native Android app shell, free/paid flavor structure, CRA review rules, backup/export/provider contracts, public documentation, GitHub templates, and version tooling.
- detail: Establishes the reusable Chris development standard for documentation, versioning, verification, GitHub publishing, and memory closeout.

## 0.2.0 - 2026-05-25

- minor: add the local trip ledger MVP.
- detail: Adds file-backed Android ledger storage for vehicles and manual trip records.
- detail: Adds Android screens for summary, vehicle entry, manual trip entry, review queue, trip ledger, and local CSV export.
- detail: Adds shared ledger summary and CRA-style CSV export logic with tests.
- detail: Documents the local MVP storage path and updates release expectations for UI smoke testing.

## 0.3.0 - 2026-05-25

- minor: make the local ledger workflow more usable.
- detail: Adds review queue actions to classify trips as business or personal and delete bad entries.
- detail: Adds trip deletion from the full ledger list.
- detail: Adds Android FileProvider support and share-sheet export for CRA-style CSV logbooks and JSON backups.
- detail: Adds local corrupt-ledger recovery so malformed private storage is preserved and the app can start cleanly.
- detail: Uses Android BuildConfig version metadata for the app header instead of a hardcoded version string.

## 0.4.0 - 2026-05-27

- minor: add date-range review and job/category tagging.
- detail: Adds a review date-range panel so pending and mixed trips can be reviewed for a targeted period.
- detail: Adds job/client and category labels to reviewed trips and persists those labels in the local ledger.
- detail: Expands manual trip entry to capture explicit date, start time, end time, start point, and end point.
- detail: Adds job and category fields to CSV exports for downstream ERPNext-ready classification.
- detail: Updates shared tests and documentation for the v0.4.0 review-classification workflow.

## 0.4.1 - 2026-05-27

- patch: fix Android system bar overlap.
- detail: Applies safe drawing insets to the main Compose scroll layout so content no longer bleeds into the top status bar or bottom system gesture area.

## 0.5.0 - 2026-05-27

- minor: add tabbed Android UX and vehicle deletion.
- detail: Replaces the single long Android page with tabs for Trips, Review, Vehicles, and Export.
- detail: Adds vehicle deletion in the Vehicles tab.
- detail: Preserves trip history when a vehicle is deleted by clearing matching trip vehicle references instead of deleting trips.
- detail: Updates shared tests and documentation for the tabbed v0.5.0 workflow.

## 0.6.0 - 2026-05-27

- minor: add appearance settings and dark mode.
- detail: Adds a Settings tab with Follow system, Light, and Dark theme options.
- detail: Defaults new installs to Follow system.
- detail: Persists the selected appearance mode in private Android preferences.
- detail: Applies Material light and dark color schemes across the Android app.

## 0.7.0 - 2026-05-27

- minor: add encrypted backup archive support.
- detail: Adds Android Keystore AES-GCM encrypted `.qfmbackup` archive creation.
- detail: Adds latest encrypted backup restore from the Export tab.
- detail: Keeps debug JSON backup sharing but labels encrypted backup as the Drive-ready path.
- detail: Excludes raw ledger, CSV, JSON backup, and encrypted archive files from generic Android Auto Backup/device transfer.

## 0.7.1 - 2026-05-27

- patch: align Android theme with QuantumForm brand colors.
- detail: Replaces default Material purple accents with QuantumForm Accent Gold `#C58A2F`, Primary Black `#111111`, and White `#FFFFFF`.
- detail: Keeps light, dark, and Follow system appearance modes while applying the brand palette across buttons, tabs, fields, and app surfaces.
