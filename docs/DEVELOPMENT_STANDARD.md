# Chris Development Standard

This is the reusable development standard for Chris's software projects.

## Definition Of Done

A change is not done until it is:

- implemented in the smallest reasonable scope;
- documented where future maintainers will look;
- versioned with a deliberate SemVer bump;
- verified with automated checks or explicit manual evidence;
- checked for secrets and private data before public push;
- committed locally and pushed to the configured GitHub remote when publishing is part of the request;
- captured to shared memory when the work creates durable project or workflow knowledge.

## Documentation Style

Prefer docs-heavy, code-normal:

- README for product status, setup, and operating context.
- Architecture docs for decisions and boundaries.
- Data docs for schema, field meaning, retention, and export semantics.
- Privacy/security docs for sensitive surfaces and rules.
- Changelog for every meaningful release.
- ADRs for durable platform or architecture choices.

Code comments should explain why a rule exists, where privacy or tax evidence matters, or how an adapter boundary protects portability. Do not narrate obvious assignments or framework boilerplate.

## Versioning Style

Every completed change gets at least a patch bump.

- Patch: bug fixes, copy/docs corrections, minor cleanup, small internal refinements.
- Minor: features, user-visible workflow changes, new exports, new adapters, notable docs/release automation.
- Major: breaking data/export changes, store-release milestones, large architecture replacement, public positioning changes.

The version source of truth is `package.json`; Android `versionName` must match it.

## Verification Style

Verification should match risk:

- Shared business logic gets unit tests.
- Platform integration gets adapter tests or documented manual evidence.
- Public UI changes get screenshot/viewport checks when practical.
- Privacy-sensitive flows get redaction and secret-scan coverage.
- Backup/export changes get restore/import round-trip proof.

If a local tool is missing, say exactly what could not be verified and why.

## GitHub Style

GitHub documentation should be useful to a stranger seeing the public repo:

- No boilerplate README/license left in place.
- No secrets or private data.
- PR templates ask for privacy/security and verification notes.
- CI should check docs, version consistency, and tests when repository credentials allow workflow files to be published.
- Public source-available repos should omit a license until reuse rights are deliberately chosen.

## Memory Style

Use memory at decision time, not as a giant always-loaded prompt. Capture durable project standards, release workflows, architecture decisions, and user preferences. Store structured summaries, not raw transcripts.
