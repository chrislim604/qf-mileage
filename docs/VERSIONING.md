# Versioning

QF Mileage uses SemVer.

The version source of truth is `package.json`. Android `versionName` in `app/build.gradle.kts` and the root entry in `package-lock.json` must match.

## Bump Rules

- Patch: small fixes, docs corrections, dependency-only maintenance, tiny internal cleanup.
- Minor: features, default-visible behavior, new exports, new adapters, new docs/release automation.
- Major: breaking data or export changes, store-release milestones, architecture replacement, or incompatible backup format changes.

Every completed change gets at least a patch bump.

## Commands

```sh
npm run version:bump
npm run version:bump:patch
npm run version:bump:minor
npm run version:bump:major
npm run version:check
```

Version bumps should update `package.json`, `package-lock.json`, Android `versionName`, and `CHANGELOG.md`.
