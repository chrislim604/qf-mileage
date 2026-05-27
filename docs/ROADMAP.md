# Roadmap

QF Mileage should grow as a privacy-conscious, review-first mileage app. The core product direction is to reuse user-controlled evidence, keep trip review explicit, and produce clean CRA and ERPNext records without requiring an always-on location tracker.

## Product Principle

The app should not treat automation as authority. Imports, route calculations, saved places, Bluetooth hints, and later background assistance can suggest trip records, but the user reviews and approves claimable business mileage before export.

## Current Release

### 0.8.0 - Timeline Import

- Add a dedicated Import tab.
- Let Android users select a Google Timeline or Takeout-style JSON file.
- Parse supported driving activity segments in shared KMP core.
- Import trips as `GoogleTimelineImport` source records with `NeedsReview` purpose.
- De-duplicate repeated imports using deterministic Timeline trip IDs.
- Keep imported trips local until the user exports or backs up.

## Near-Term Releases

### 0.9.0 - Places And Auto-Tagging

- Add approved saved places for Home, Office, Warehouse, Client, Supplier, Personal, and Custom.
- Suggest frequent places from reviewed trips.
- Add user-visible auto-tagging rules for recurring destinations, jobs, and categories.
- Keep every suggested place or rule approval-based before it affects exports.
- Initial implementation covers saved places, frequent endpoint suggestions, and explicit label tagging. Recurring job/category rules remain future work.

### 0.10.0 - ERPNext Export Batches

- Add ERPNext-ready CSV/JSON export batches for approved business trips.
- Mark exported trips with batch metadata to prevent duplicate submissions.
- Shape the contract for Expense Claim first, with Vehicle Log support added later if useful.
- Include CRA-style evidence fields alongside ERPNext mapping fields.

## Later MVP Extensions

- Trip split and merge for real-life errands, detours, and accidental combined imports.
- Google Routes driven-distance reconstruction when imported evidence is incomplete or jumpy.
- Google Drive `appDataFolder` upload/download for encrypted `.qfmbackup` archives.
- Odometer log by vehicle for stronger yearly recordkeeping.
- Review reminders for unclassified imported trips.
- Bluetooth vehicle hints from approved paired-device observations.
- Receipt and expense capture for parking, tolls, fuel, meals, and related business expenses.

## Monetization And Store Work

- Free app: banner ads during normal use and transition-only interstitials around review/export flows.
- Free app remove-ads entitlement: one-time in-app purchase.
- Paid app: separate up-front ad-free SKU.
- Do not integrate live AdMob or Play Billing identifiers until the UX and privacy flows are stable enough for store configuration.

## Deliberately Deferred

- Bank/card import, because it expands privacy and security scope substantially.
- Team/fleet approvals, because the single-user workflow must be dependable first.
- Full automatic GPS tracking, because the first product bet is Timeline import plus review rather than battery-heavy background tracking.
