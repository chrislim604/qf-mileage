# Release Checklist

Before finishing a meaningful change:

- [ ] Docs updated where behavior, data, privacy, backup, ads, billing, export, or operations changed.
- [ ] Version bumped with the correct SemVer level.
- [ ] `CHANGELOG.md` updated.
- [ ] `npm run docs:check` passes.
- [ ] `npm run version:check` passes.
- [ ] `npm test` passes.
- [ ] Full Android build run when Android platform code changes and the SDK is available.
- [ ] `npm run secrets:scan` passes before public push.
- [ ] Local git commit created.
- [ ] GitHub push completed when publishing is requested.
- [ ] Shared memory closeout captured for durable decisions or workflow changes.
