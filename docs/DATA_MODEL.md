# Data Model

The data model keeps the internal trip ledger canonical so exports and ERPNext integrations can evolve without losing evidence.

## Core Records

- Vehicle: display name, optional odometer metadata, and approved Bluetooth device mappings.
- PlaceTag: user-approved frequent place labels such as Home, Office, Warehouse, Client, Supplier, Personal, or Custom.
- TripLeg: start/end times, origin/destination labels, kilometres, vehicle, optional job/client label, optional category label, purpose, source, evidence note, and optional adjustment note.
- RouteDistanceResult: driven-distance calculation, provider, confidence, and evidence summary.
- BackupManifest: schema version, creation time, encryption flag, and record counts for JSON and encrypted backup archives.
- Entitlement: `FreeWithAds`, `FreeAdRemoved`, or `PaidAdFree`.
- LedgerSnapshot: the local collection of vehicles, places, and trips.
- TimelineImportResult: normalized output from a user-selected Timeline JSON file, including imported trips, skipped segment count, and optional warning.

## Evidence Rules

Do not replace observed trip evidence with a cleaned-up answer. Preserve:

- original source type;
- route provider used for reconstruction;
- confidence;
- user decisions;
- adjustment notes;
- export history when implemented.

## CRA Review Rules

Mixed or adjusted trips require user notes before export. The app is a decision-support ledger, not tax advice.

## ERPNext Boundary

ERPNext integration remains contract-first until the real ERPNext instance exists. The internal ledger should be able to export Expense Claim-style records first, with Vehicle Log support added later if useful.

## Local MVP Storage

The Android v0.7.0 MVP stores `LedgerSnapshot` data in a private JSON file. This is intentionally simple and local-only. The next persistence upgrade can move the same records into SQLDelight or Room without changing the shared core concepts.

Manual trip records can be entered with explicit local date, start time, end time, start point, end point, vehicle, job/client, category, kilometres, and evidence note. Date-range review filters use the trip start date so a user can focus review on a billing period, payroll period, or catch-up window without changing the canonical ledger.

Timeline imports create deterministic `TripLeg` IDs from start/end time and endpoint coordinates. This lets users safely import the same Timeline file more than once without creating duplicate review trips. Imported trips always use `TripSource.GoogleTimelineImport`, start as `TripPurpose.NeedsReview`, and carry an evidence note describing the imported segment type.

Deleting a vehicle removes the vehicle record and clears matching `TripLeg.vehicleId` references. It does not delete trips, because trip history is evidence and should survive settings cleanup.

If the local JSON file cannot be parsed, the app preserves it as a timestamped `.corrupt-...json` file and starts with a clean default ledger instead of crashing on launch.

Encrypted `.qfmbackup` archives wrap the current ledger JSON in an archive object with a manifest, algorithm name, initialization vector, and AES-GCM encrypted payload. Android creates the encryption key in Android Keystore so future Google Drive `appDataFolder` sync can upload encrypted archives rather than raw trip data.
