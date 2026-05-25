# Codex Instructions For QF Mileage

This repository is public/source-available and may contain architecture for sensitive location, billing, advertising, and backup flows. Keep privacy and release hygiene tight.

## Required Workflow

- Use shared AI memory before important architecture, release, or project-standard decisions.
- Every completed change must include at least a patch version bump.
- Use `npm run version:bump:minor` for feature/default-visible behavior changes unless the change is clearly tiny.
- Use `npm run version:bump:major` for breaking data/export changes or milestone/store releases.
- Update docs in the same change when behavior, data shape, privacy posture, platform setup, ads, billing, backups, exports, or release workflow changes.
- Run and report the relevant checks before finishing:
  - `npm run docs:check`
  - `npm run version:check`
  - `gradle :shared:allTests`
  - `npm run secrets:scan` before commit/push when available
- GitHub push is part of done when the user asks for backup, publishing, or release completion.

## Privacy Rules

- Do not commit real Timeline exports, trip history, addresses, GPS traces, app backups, screenshots, billing IDs, ad unit IDs, OAuth secrets, API keys, keystores, or `.env` files.
- Store structured summaries and synthetic fixtures only.
- Treat location history, routes, vehicle identities, and business destinations as private data.

## Coding Style

- Keep the shared KMP core platform-neutral.
- Put Android, Google, billing, ads, Bluetooth, and backup integrations behind adapters.
- Keep code comments focused on non-obvious business rules, privacy-sensitive decisions, route reconstruction, CRA review logic, and adapter boundaries.
- Avoid comments that restate obvious code.
